package com.upc.grupo3.clients;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.upc.grupo3.config.MapboxProperties;
import com.upc.grupo3.dtos.routeevaluation.ResolvedPlaceDTO;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class MapboxClientTest {

    private MapboxClient mapboxClient;

    @BeforeEach
    void setUp() {
        MapboxProperties properties = new MapboxProperties();
        properties.setAccessToken("test-token");
        properties.setGeocodingUrl("https://api.mapbox.com/geocoding/v5/mapbox.places");
        properties.setDirectionsUrl("https://api.mapbox.com/directions/v5");
        mapboxClient = new MapboxClient(RestClient.builder(), properties);
    }

    @Test
    void buildDirectionsUriShouldUseMapboxProfileAndLongitudeLatitudeOrder() throws Exception {
        ResolvedPlaceDTO origin = ResolvedPlaceDTO.builder()
                .latitude(-12.086)
                .longitude(-76.975)
                .build();
        ResolvedPlaceDTO destination = ResolvedPlaceDTO.builder()
                .latitude(-12.104)
                .longitude(-76.963)
                .build();

        URI uri = invokeBuildDirectionsUri(origin, destination, "mapbox/walking");

        assertEquals(
                "https://api.mapbox.com/directions/v5/mapbox/walking/-76.975000,-12.086000;-76.963000,-12.104000"
                        + "?access_token=test-token&alternatives=true&steps=true&overview=full&geometries=geojson&language=es",
                uri.toString());
    }

    @Test
    void buildGeocodingUriShouldNormalizeQueryAndRestrictToPeru() throws Exception {
        URI uri = invokeBuildGeocodingUri("Parque Kennedy Miraflores", 5, false);

        assertEquals(
                "https://api.mapbox.com/geocoding/v5/mapbox.places/Parque%20Kennedy%20Miraflores,%20Lima,%20Per%C3%BA.json"
                        + "?access_token=test-token&limit=5&language=es&country=pe",
                uri.toString());
    }

    @Test
    void buildGeocodingUriShouldIncludeLimaBboxWhenRequested() throws Exception {
        URI uri = invokeBuildGeocodingUri("UPC Monterrico", 5, true);

        assertEquals(
                "https://api.mapbox.com/geocoding/v5/mapbox.places/UPC%20Monterrico,%20Lima,%20Per%C3%BA.json"
                        + "?access_token=test-token&limit=5&language=es&country=pe&bbox=-77.12,-12.25,-76.85,-11.95",
                uri.toString());
    }

    @Test
    void evaluateGeocodingMatchShouldRejectLowRelevance() throws Exception {
        String rejectionReason = invokeEvaluateGeocodingMatch(
                "asdasdasd lugar falso 123456",
                "asdasdasd lugar falso 123456, Lima, Per\u00fa",
                "origen",
                "Avenida Lima",
                "Avenida Lima, Lima, Per\u00fa",
                0.42d,
                List.of("address"));

        assertEquals("relevance_below_threshold", rejectionReason);
    }

    @Test
    void evaluateGeocodingMatchShouldRejectGenericPlaceWithoutOverlap() throws Exception {
        String rejectionReason = invokeEvaluateGeocodingMatch(
                "UPC Monterrico",
                "UPC Monterrico, Lima, Per\u00fa",
                "destino",
                "Lima",
                "Lima, Provincia de Lima, Per\u00fa",
                1.0d,
                List.of("place"));

        assertEquals("generic_place_without_query_overlap", rejectionReason);
    }

    @Test
    void evaluateGeocodingMatchShouldAcceptGenericPlaceWithUsefulOverlap() throws Exception {
        assertDoesNotThrow(() -> {
            String rejectionReason = invokeEvaluateGeocodingMatch(
                    "Parque Kennedy Miraflores",
                    "Parque Kennedy Miraflores, Lima, Per\u00fa",
                    "origen",
                    "Miraflores",
                    "Miraflores, Provincia de Lima, Per\u00fa",
                    0.718667d,
                    List.of("place"));
            assertNull(rejectionReason);
        });
    }

    private URI invokeBuildGeocodingUri(String query, int limit, boolean useLimaBbox) throws Exception {
        Method method = MapboxClient.class.getDeclaredMethod("buildGeocodingUri", String.class, int.class, boolean.class);
        method.setAccessible(true);
        return (URI) method.invoke(mapboxClient, query, limit, useLimaBbox);
    }

    private URI invokeBuildDirectionsUri(ResolvedPlaceDTO origin, ResolvedPlaceDTO destination, String profile)
            throws Exception {
        Method method = MapboxClient.class.getDeclaredMethod(
                "buildDirectionsUri",
                ResolvedPlaceDTO.class,
                ResolvedPlaceDTO.class,
                String.class);
        method.setAccessible(true);
        return (URI) method.invoke(mapboxClient, origin, destination, profile);
    }

    private String invokeEvaluateGeocodingMatch(
            String originalQuery,
            String normalizedQuery,
            String locationLabel,
            String resolvedName,
            String resolvedAddress,
            Double relevance,
            List<String> placeTypes) throws Exception {
        Method method = MapboxClient.class.getDeclaredMethod(
                "evaluateGeocodingMatch",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Double.class,
                List.class);
        method.setAccessible(true);
        return (String) method.invoke(
                mapboxClient,
                originalQuery,
                normalizedQuery,
                locationLabel,
                resolvedName,
                resolvedAddress,
                relevance,
                placeTypes);
    }
}
