package com.upc.grupo3.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.grupo3.config.MapboxProperties;
import com.upc.grupo3.dtos.routeevaluation.GeoJsonLineStringDTO;
import com.upc.grupo3.dtos.routeevaluation.ResolvedPlaceDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteOptionDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteStepDTO;
import com.upc.grupo3.exceptions.ApplicationConfigurationException;
import com.upc.grupo3.exceptions.MapboxDirectionsException;
import com.upc.grupo3.exceptions.MapboxGeocodingException;
import com.upc.grupo3.exceptions.RouteNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@Component
@Slf4j
public class MapboxClient {

    private static final double MIN_GEOCODING_RELEVANCE = 0.70d;
    private static final int GEOCODING_CANDIDATE_LIMIT = 5;
    private static final String LIMA_BBOX = "-77.12,-12.25,-76.85,-11.95";
    private static final String MAPBOX_SERVICE_ERROR_MESSAGE =
            "No se pudo obtener rutas desde el proveedor de mapas.";
    private static final String MAPBOX_GEOCODING_ERROR_MESSAGE =
            "No se pudo resolver la direccion con el proveedor de mapas.";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> GENERIC_PLACE_TYPES = Set.of("place", "region", "country");
    private static final Set<String> PREFERRED_PLACE_TYPES =
            Set.of("address", "poi", "neighborhood", "locality");
    private static final List<String> SPECIFIC_QUERY_HINTS = List.of(
            "av",
            "av.",
            "avenida",
            "calle",
            "jr",
            "jiron",
            "jir\u00f3n",
            "pasaje",
            "psje",
            "nro",
            "numero",
            "n\u00famero",
            "cuadra",
            "km",
            "direccion",
            "direcci\u00f3n");
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "lima",
            "peru",
            "provincia",
            "departamento",
            "del",
            "de",
            "la",
            "el",
            "los",
            "las",
            "y");

    private final RestClient restClient;
    private final MapboxProperties mapboxProperties;

    public MapboxClient(RestClient.Builder restClientBuilder, MapboxProperties mapboxProperties) {
        this.restClient = restClientBuilder.build();
        this.mapboxProperties = mapboxProperties;
    }

    public ResolvedPlaceDTO geocode(String query, String locationLabel) {
        validateConfiguration();

        String originalQuery = query.trim();
        String normalizedQuery = normalizeQuery(originalQuery);

        MapboxFeature feature = selectFeatureForQuery(originalQuery, normalizedQuery, locationLabel, false);
        if (feature == null && !normalizedQuery.equals(originalQuery)) {
            feature = selectFeatureForQuery(originalQuery, originalQuery, locationLabel, true);
        }
        if (feature == null) {
            log.warn("Geocoding sin resultados validos locationLabel={} query={} normalizedQuery={}",
                    locationLabel,
                    originalQuery,
                    normalizedQuery);
            throw new RouteNotFoundException(
                    "No se pudo encontrar una ubicacion valida para el " + locationLabel + " ingresado.",
                    "GEOCODING_NOT_FOUND");
        }

        String resolvedName = StringUtils.hasText(feature.text()) ? feature.text() : feature.placeName();
        if (feature.center() == null || feature.center().size() < 2) {
            log.error("Geocoding Mapbox devolvio coordenadas incompletas query={} feature={}", query, feature);
            throw new MapboxGeocodingException(MAPBOX_GEOCODING_ERROR_MESSAGE);
        }

        ResolvedPlaceDTO resolvedPlace = ResolvedPlaceDTO.builder()
                .name(resolvedName)
                .address(feature.placeName())
                .latitude(feature.center().get(1))
                .longitude(feature.center().get(0))
                .build();

        log.info("Geocoding resuelto locationLabel={} coordinates={},{} address='{}'",
                locationLabel,
                resolvedPlace.getLongitude(),
                resolvedPlace.getLatitude(),
                resolvedPlace.getAddress());
        return resolvedPlace;
    }

    public List<RouteOptionDTO> getDirections(
            ResolvedPlaceDTO origin,
            ResolvedPlaceDTO destination,
            String transportMode) {
        validateConfiguration();

        String profile = resolveDirectionsProfile(transportMode);
        URI uri = buildDirectionsUri(origin, destination, profile);
        MapboxDirectionsResponse response;

        log.info("Mapbox directions profile={}", profile);
        log.info("Mapbox directions originCoordinates={},{} destinationCoordinates={},{}",
                origin.getLongitude(),
                origin.getLatitude(),
                destination.getLongitude(),
                destination.getLatitude());
        log.info("Mapbox directions url={}", sanitizeUri(uri));

        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(MapboxDirectionsResponse.class);
        } catch (RestClientResponseException exception) {
            log.error("Error HTTP en directions Mapbox status={} mapboxError={} origin={},{} destination={},{} mode={} profile={}",
                    exception.getStatusCode().value(),
                    extractMapboxError(exception.getResponseBodyAsString()),
                    origin.getLongitude(),
                    origin.getLatitude(),
                    destination.getLongitude(),
                    destination.getLatitude(),
                    transportMode,
                    profile);
            throw new MapboxDirectionsException(MAPBOX_SERVICE_ERROR_MESSAGE);
        } catch (RestClientException exception) {
            log.error("Error de integracion en directions Mapbox origin={},{} destination={},{} mode={} profile={}",
                    origin.getLongitude(),
                    origin.getLatitude(),
                    destination.getLongitude(),
                    destination.getLatitude(),
                    transportMode,
                    profile,
                    exception);
            throw new MapboxDirectionsException(MAPBOX_SERVICE_ERROR_MESSAGE);
        }

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            log.warn("Directions sin rutas origin={} destination={} mode={}",
                    origin.getAddress(), destination.getAddress(), transportMode);
            throw new RouteNotFoundException(
                    "No se encontraron rutas entre el origen y el destino indicados.",
                    "ROUTE_NOT_FOUND");
        }

        log.info("Mapbox directions routesCount={}", response.routes().size());

        List<RouteOptionDTO> routes = new ArrayList<>();
        for (int index = 0; index < response.routes().size(); index++) {
            MapboxRoute route = response.routes().get(index);
            if (route.geometry() == null || route.geometry().coordinates() == null
                    || route.geometry().coordinates().isEmpty()) {
                log.error("Directions Mapbox devolvio una ruta sin geometria index={}", index);
                throw new MapboxDirectionsException(MAPBOX_SERVICE_ERROR_MESSAGE);
            }

            routes.add(RouteOptionDTO.builder()
                    .routeId("route_" + (index + 1))
                    .summary(StringUtils.hasText(route.summary()) ? route.summary() : "Ruta " + (index + 1))
                    .durationMinutes(round(route.duration() / 60d, 1))
                    .distanceKm(round(route.distance() / 1000d, 1))
                    .geometry(GeoJsonLineStringDTO.builder()
                            .type(route.geometry().type())
                            .coordinates(route.geometry().coordinates())
                            .build())
                    .steps(extractSteps(route.legs()))
                    .build());
        }

        return routes;
    }

    private MapboxFeature selectFeatureForQuery(
            String originalQuery,
            String queryToSend,
            String locationLabel,
            boolean useLimaBbox) {
        MapboxGeocodingResponse response = fetchGeocodingResponse(originalQuery, queryToSend, useLimaBbox);
        if (response == null || response.features() == null || response.features().isEmpty()) {
            return null;
        }

        for (MapboxFeature feature : response.features()) {
            String resolvedName = StringUtils.hasText(feature.text()) ? feature.text() : feature.placeName();
            String rejectionReason = evaluateGeocodingMatch(
                    originalQuery,
                    queryToSend,
                    locationLabel,
                    resolvedName,
                    feature.placeName(),
                    feature.relevance(),
                    feature.placeType());
            if (rejectionReason == null) {
                return feature;
            }
        }

        return null;
    }

    private MapboxGeocodingResponse fetchGeocodingResponse(
            String originalQuery,
            String queryToSend,
            boolean useLimaBbox) {
        URI uri = buildGeocodingUri(queryToSend, GEOCODING_CANDIDATE_LIMIT, useLimaBbox);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(MapboxGeocodingResponse.class);
        } catch (RestClientResponseException exception) {
            log.error("Error HTTP en geocoding Mapbox status={} mapboxError={} query={} normalizedQuery={} useLimaBbox={}",
                    exception.getStatusCode().value(),
                    extractMapboxError(exception.getResponseBodyAsString()),
                    originalQuery,
                    queryToSend,
                    useLimaBbox);
            throw new MapboxGeocodingException(MAPBOX_GEOCODING_ERROR_MESSAGE);
        } catch (RestClientException exception) {
            log.error("Error de integracion en geocoding Mapbox query={} normalizedQuery={} useLimaBbox={}",
                    originalQuery,
                    queryToSend,
                    useLimaBbox,
                    exception);
            throw new MapboxGeocodingException(MAPBOX_GEOCODING_ERROR_MESSAGE);
        }
    }

    private String evaluateGeocodingMatch(
            String originalQuery,
            String normalizedQuery,
            String locationLabel,
            String resolvedName,
            String resolvedAddress,
            Double relevance,
            List<String> placeTypes) {
        List<String> normalizedPlaceTypes = normalizePlaceTypes(placeTypes);
        boolean specificQuery = isSpecificQuery(originalQuery);
        boolean genericPlaceTypeOnly = isGenericPlaceTypeOnly(normalizedPlaceTypes);
        boolean hasPreferredSpecificType = hasPreferredSpecificType(normalizedPlaceTypes);
        boolean resolvedAsRegionOrCountry = normalizedPlaceTypes.contains("region")
                || normalizedPlaceTypes.contains("country");

        log.info(
                "Mapbox geocoding candidate locationLabel={} query='{}' normalizedQuery='{}' resolvedName='{}' resolvedAddress='{}' relevance={} placeType={}",
                locationLabel,
                originalQuery,
                normalizedQuery,
                resolvedName,
                resolvedAddress,
                relevance,
                normalizedPlaceTypes);

        if (relevance != null && relevance < MIN_GEOCODING_RELEVANCE) {
            return logGeocodingRejection(
                    locationLabel, originalQuery, normalizedQuery, resolvedName, resolvedAddress,
                    relevance, normalizedPlaceTypes, "relevance_below_threshold");
        }

        if (resolvedAsRegionOrCountry && genericPlaceTypeOnly) {
            return logGeocodingRejection(
                    locationLabel, originalQuery, normalizedQuery, resolvedName, resolvedAddress,
                    relevance, normalizedPlaceTypes, "generic_region_or_country_result");
        }

        int tokenOverlap = countTokenOverlap(originalQuery, resolvedName, resolvedAddress);
        if (genericPlaceTypeOnly && normalizedPlaceTypes.contains("place") && tokenOverlap == 0) {
            return logGeocodingRejection(
                    locationLabel, originalQuery, normalizedQuery, resolvedName, resolvedAddress,
                    relevance, normalizedPlaceTypes, "generic_place_without_query_overlap");
        }

        if (genericPlaceTypeOnly && normalizedPlaceTypes.contains("place") && specificQuery && tokenOverlap < 2) {
            return logGeocodingRejection(
                    locationLabel, originalQuery, normalizedQuery, resolvedName, resolvedAddress,
                    relevance, normalizedPlaceTypes, "specific_street_query_resolved_to_generic_place");
        }

        if (!hasPreferredSpecificType
                && specificQuery
                && isGenericResolvedName(resolvedName)
                && (genericPlaceTypeOnly || normalizedPlaceTypes.isEmpty())) {
            return logGeocodingRejection(
                    locationLabel, originalQuery, normalizedQuery, resolvedName, resolvedAddress,
                    relevance, normalizedPlaceTypes, "specific_query_resolved_to_generic_name");
        }

        return null;
    }

    private List<RouteStepDTO> extractSteps(List<MapboxLeg> legs) {
        List<RouteStepDTO> steps = new ArrayList<>();
        if (legs == null || legs.isEmpty()) {
            return steps;
        }

        int order = 1;
        for (MapboxLeg leg : legs) {
            if (leg.steps() == null || leg.steps().isEmpty()) {
                continue;
            }

            for (MapboxStep step : leg.steps()) {
                steps.add(RouteStepDTO.builder()
                        .order(order++)
                        .instruction(step.maneuver() != null ? step.maneuver().instruction() : null)
                        .streetName(StringUtils.hasText(step.name()) ? step.name() : null)
                        .distanceMeters(step.distance())
                        .durationSeconds(step.duration())
                        .maneuverType(step.maneuver() != null ? step.maneuver().type() : null)
                        .modifier(step.maneuver() != null ? step.maneuver().modifier() : null)
                        .build());
            }
        }

        return steps;
    }

    private URI buildGeocodingUri(String query, int limit, boolean useLimaBbox) {
        String encodedQuery = UriUtils.encodePathSegment(normalizeQuery(query), StandardCharsets.UTF_8);
        String baseUrl = trimTrailingSlash(mapboxProperties.getGeocodingUrl());
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/" + encodedQuery + ".json")
                .queryParam("access_token", mapboxProperties.getAccessToken())
                .queryParam("limit", limit)
                .queryParam("language", "es")
                .queryParam("country", "pe");
        if (useLimaBbox) {
            builder.queryParam("bbox", LIMA_BBOX);
        }
        return builder.build(true).toUri();
    }

    private URI buildDirectionsUri(
            ResolvedPlaceDTO origin,
            ResolvedPlaceDTO destination,
            String profile) {
        String baseUrl = trimTrailingSlash(mapboxProperties.getDirectionsUrl());
        String coordinates = String.format(
                Locale.US,
                "%.6f,%.6f;%.6f,%.6f",
                origin.getLongitude(),
                origin.getLatitude(),
                destination.getLongitude(),
                destination.getLatitude());

        return UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/" + profile + "/" + coordinates)
                .queryParam("access_token", mapboxProperties.getAccessToken())
                .queryParam("alternatives", true)
                .queryParam("steps", true)
                .queryParam("overview", "full")
                .queryParam("geometries", "geojson")
                .queryParam("language", "es")
                .build(true)
                .toUri();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(mapboxProperties.getAccessToken())) {
            throw new ApplicationConfigurationException(
                    "La variable de entorno MAPBOX_ACCESS_TOKEN no esta configurada");
        }
        if (!StringUtils.hasText(mapboxProperties.getGeocodingUrl())
                || !StringUtils.hasText(mapboxProperties.getDirectionsUrl())) {
            throw new ApplicationConfigurationException(
                    "La configuracion base de Mapbox no esta completa");
        }
    }

    private String normalizeQuery(String query) {
        String value = query.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("per\u00fa") || lower.contains("peru")) {
            return value;
        }
        return value + ", Lima, Per\u00fa";
    }

    private String resolveDirectionsProfile(String transportMode) {
        return "mapbox/" + transportMode.trim().toLowerCase(Locale.ROOT);
    }

    private String logGeocodingRejection(
            String locationLabel,
            String originalQuery,
            String normalizedQuery,
            String resolvedName,
            String resolvedAddress,
            Double relevance,
            List<String> placeTypes,
            String rejectionReason) {
        log.warn(
                "Mapbox geocoding rechazado locationLabel={} query='{}' normalizedQuery='{}' resolvedName='{}' resolvedAddress='{}' relevance={} placeType={} rejectionReason={}",
                locationLabel,
                originalQuery,
                normalizedQuery,
                resolvedName,
                resolvedAddress,
                relevance,
                placeTypes,
                rejectionReason);
        return rejectionReason;
    }

    private List<String> normalizePlaceTypes(List<String> placeTypes) {
        if (placeTypes == null || placeTypes.isEmpty()) {
            return List.of();
        }
        return placeTypes.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    private boolean isGenericPlaceTypeOnly(List<String> placeTypes) {
        return !placeTypes.isEmpty() && placeTypes.stream().allMatch(GENERIC_PLACE_TYPES::contains);
    }

    private boolean hasPreferredSpecificType(List<String> placeTypes) {
        return placeTypes.stream().anyMatch(PREFERRED_PLACE_TYPES::contains);
    }

    private boolean isSpecificQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }

        String normalized = normalizeComparableText(query);
        if (normalized.chars().anyMatch(Character::isDigit)) {
            return true;
        }

        return SPECIFIC_QUERY_HINTS.stream()
                .map(this::normalizeComparableText)
                .anyMatch(hint -> containsWordish(normalized, hint));
    }

    private int countTokenOverlap(String query, String resolvedName, String resolvedAddress) {
        List<String> queryTokens = extractMeaningfulTokens(query);
        if (queryTokens.isEmpty()) {
            return 0;
        }

        String candidateText = normalizeComparableText(
                (resolvedName != null ? resolvedName : "") + " " + (resolvedAddress != null ? resolvedAddress : ""));
        int overlap = 0;
        for (String token : queryTokens) {
            if (containsWordish(candidateText, token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private List<String> extractMeaningfulTokens(String query) {
        String normalized = normalizeComparableText(query);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        return Arrays.stream(normalized.split("\\s+"))
                .filter(StringUtils::hasText)
                .filter(token -> token.length() >= 3 || token.chars().allMatch(Character::isDigit))
                .filter(token -> !QUERY_STOP_WORDS.contains(token))
                .toList();
    }

    private boolean isGenericResolvedName(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String normalized = normalizeComparableText(value);
        return Arrays.stream(normalized.split("\\s+"))
                .filter(StringUtils::hasText)
                .count() <= 2;
    }

    private String normalizeComparableText(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replace('\u00e1', 'a')
                        .replace('\u00e9', 'e')
                        .replace('\u00ed', 'i')
                        .replace('\u00f3', 'o')
                        .replace('\u00fa', 'u')
                        .replaceAll("[^a-z0-9\\s.]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
    }

    private boolean containsWordish(String normalizedText, String normalizedHint) {
        if (!StringUtils.hasText(normalizedText) || !StringUtils.hasText(normalizedHint)) {
            return false;
        }

        String paddedText = " " + normalizedText + " ";
        String paddedHint = " " + normalizedHint + " ";
        return paddedText.contains(paddedHint)
                || normalizedText.startsWith(normalizedHint + " ")
                || normalizedText.contains(" " + normalizedHint + ".")
                || normalizedText.contains(" " + normalizedHint + " ");
    }

    private String sanitizeUri(URI uri) {
        String value = uri.toString();
        return value.replaceAll("([?&])access_token=[^&]*&?", "$1")
                .replace("?&", "?")
                .replaceAll("[?&]$", "");
    }

    private String extractMapboxError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "(sin detalle)";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String message = root.path("message").asText(null);
            String code = root.path("code").asText(null);
            if (StringUtils.hasText(code) && StringUtils.hasText(message)) {
                return code + ": " + message;
            }
            if (StringUtils.hasText(message)) {
                return message;
            }
            if (StringUtils.hasText(code)) {
                return code;
            }
        } catch (Exception exception) {
            log.debug("No se pudo parsear el error de Mapbox", exception);
        }

        return responseBody;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxGeocodingResponse(List<MapboxFeature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxFeature(
            String text,
            @JsonProperty("place_name")
            String placeName,
            Double relevance,
            @JsonProperty("place_type")
            List<String> placeType,
            List<Double> center) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxDirectionsResponse(List<MapboxRoute> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxRoute(
            String summary,
            Double distance,
            Double duration,
            MapboxGeometry geometry,
            List<MapboxLeg> legs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxGeometry(
            String type,
            List<List<Double>> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxLeg(List<MapboxStep> steps) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxStep(
            String name,
            Double distance,
            Double duration,
            MapboxManeuver maneuver) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MapboxManeuver(
            String instruction,
            String type,
            String modifier) {
    }
}
