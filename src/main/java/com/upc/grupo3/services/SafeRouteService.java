package com.upc.grupo3.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.grupo3.dtos.riskzone.RiskZoneGeometryDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneLocationDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteGeometryDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteOptionDTO;
import com.upc.grupo3.dtos.saferoute.SafeRoutePointDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteRequestDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteResponseDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteRiskZoneDTO;
import com.upc.grupo3.entidades.Ruta;
import com.upc.grupo3.entidades.RutaZona;
import com.upc.grupo3.entidades.RutaZonaId;
import com.upc.grupo3.entidades.Ubicacion;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.entidades.ZonaRiesgo;
import com.upc.grupo3.exceptions.AccountDisabledException;
import com.upc.grupo3.exceptions.ApplicationConfigurationException;
import com.upc.grupo3.exceptions.GeographicDataNotAvailableException;
import com.upc.grupo3.exceptions.InvalidSafeRouteRequestException;
import com.upc.grupo3.exceptions.ResourceNotFoundException;
import com.upc.grupo3.exceptions.UnauthenticatedUserException;
import com.upc.grupo3.repositories.RutaRepository;
import com.upc.grupo3.repositories.RutaZonaRepository;
import com.upc.grupo3.repositories.UbicacionRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.repositories.ZonaRiesgoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafeRouteService {

    private static final int LOW_RISK_LEVEL = 1;
    private static final int MEDIUM_RISK_LEVEL = 2;
    private static final int HIGH_RISK_LEVEL = 3;
    private static final int LOW_RISK_SCORE = 10;
    private static final int MEDIUM_RISK_SCORE = 45;
    private static final int HIGH_RISK_SCORE = 80;
    private static final double WALKING_SPEED_METERS_PER_MINUTE = 83.3333333333d;
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double EPSILON = 1e-9d;
    private static final BigDecimal COORDINATE_MARGIN = new BigDecimal("0.0015000");

    private final RutaRepository rutaRepository;
    private final RutaZonaRepository rutaZonaRepository;
    private final ZonaRiesgoRepository zonaRiesgoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SafeRouteResponseDTO calculateSafeRoute(
            String authenticatedEmail,
            SafeRouteRequestDTO request) {
        Usuario usuario = resolveActiveAuthenticatedUser(authenticatedEmail);
        SafeRouteRequestDTO normalizedRequest = normalizeAndValidateRequest(request);
        validateGeographicDataAvailability();

        List<ZonaRiesgo> activeZones = zonaRiesgoRepository
                .findByEstadoTrueOrderByNivelRiesgoDescFechaActualizacionDesc();
        Map<Integer, Ubicacion> locationsById = loadLocationsById(activeZones);

        RouteAlternative fastestRoute = evaluateRoute(
                buildDirectGeometry(normalizedRequest),
                activeZones,
                locationsById);
        RouteAlternative safestRoute = buildSafestRoute(
                normalizedRequest,
                activeZones,
                locationsById,
                fastestRoute);
        RouteAlternative recommendedRoute = selectRecommendedRoute(fastestRoute, safestRoute);

        SafeRouteResponseDTO response = buildResponse(
                normalizedRequest,
                fastestRoute,
                safestRoute,
                recommendedRoute,
                buildRecommendation(fastestRoute, safestRoute, recommendedRoute));

        Ruta savedRoute = persistRoute(
                usuario,
                normalizedRequest,
                recommendedRoute,
                response,
                activeZones);

        log.info("Rutas calculadas email={} rutaId={} scoreRapida={} scoreSegura={} scoreRecomendada={}",
                authenticatedEmail,
                savedRoute.getIdRuta(),
                fastestRoute.scoreRiesgo(),
                safestRoute.scoreRiesgo(),
                recommendedRoute.scoreRiesgo());

        return response;
    }

    private SafeRouteResponseDTO buildResponse(
            SafeRouteRequestDTO normalizedRequest,
            RouteAlternative fastestRoute,
            RouteAlternative safestRoute,
            RouteAlternative recommendedRoute,
            String recommendation) {
        return SafeRouteResponseDTO.builder()
                .origen(normalizedRequest.getOrigen())
                .destino(normalizedRequest.getDestino())
                .rutaMasRapida(toRouteOption(fastestRoute))
                .rutaMasSegura(toRouteOption(safestRoute))
                .rutaRecomendada(toRouteOption(recommendedRoute))
                .nivelRiesgo(resolveRiskLevelName(recommendedRoute.nivelRiesgo()))
                .scoreRiesgo(recommendedRoute.scoreRiesgo())
                .tiempoEstimado(recommendedRoute.tiempoEstimado())
                .distancia(recommendedRoute.distancia())
                .recomendacion(recommendation)
                .build();
    }

    private SafeRouteOptionDTO toRouteOption(RouteAlternative routeAlternative) {
        return SafeRouteOptionDTO.builder()
                .distancia(routeAlternative.distancia())
                .tiempoEstimado(routeAlternative.tiempoEstimado())
                .scoreRiesgo(routeAlternative.scoreRiesgo())
                .nivelRiesgo(resolveRiskLevelName(routeAlternative.nivelRiesgo()))
                .cruzaZonasRiesgo(!routeAlternative.zonasRiesgo().isEmpty())
                .geometria(routeAlternative.geometria())
                .zonasRiesgo(routeAlternative.zonasRiesgo())
                .build();
    }

    private RouteAlternative buildSafestRoute(
            SafeRouteRequestDTO request,
            List<ZonaRiesgo> activeZones,
            Map<Integer, Ubicacion> locationsById,
            RouteAlternative fastestRoute) {
        if (fastestRoute.zonasRiesgo().isEmpty()) {
            return fastestRoute;
        }

        RouteAlternative bestCandidate = fastestRoute;
        for (SafeRouteGeometryDTO candidateGeometry : buildDetourCandidates(request, activeZones, fastestRoute)) {
            RouteAlternative candidate = evaluateRoute(candidateGeometry, activeZones, locationsById);
            if (isBetterSafeCandidate(candidate, bestCandidate)) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    private boolean isBetterSafeCandidate(RouteAlternative candidate, RouteAlternative currentBest) {
        if (candidate.scoreRiesgo() != currentBest.scoreRiesgo()) {
            return candidate.scoreRiesgo() < currentBest.scoreRiesgo();
        }
        if (candidate.tiempoEstimado() != currentBest.tiempoEstimado()) {
            return candidate.tiempoEstimado() < currentBest.tiempoEstimado();
        }
        if (candidate.distancia() != currentBest.distancia()) {
            return candidate.distancia() < currentBest.distancia();
        }
        return candidate.geometria().getCoordinates().size() < currentBest.geometria().getCoordinates().size();
    }

    private List<SafeRouteGeometryDTO> buildDetourCandidates(
            SafeRouteRequestDTO request,
            List<ZonaRiesgo> activeZones,
            RouteAlternative fastestRoute) {
        Map<Integer, ZonaRiesgo> zonesById = new HashMap<>();
        activeZones.forEach(zone -> zonesById.put(zone.getIdZona(), zone));

        GeoPoint origin = toGeoPoint(List.of(
                request.getOrigen().getLongitud(),
                request.getOrigen().getLatitud()));
        GeoPoint destination = toGeoPoint(List.of(
                request.getDestino().getLongitud(),
                request.getDestino().getLatitud()));
        double routeDx = destination.longitude - origin.longitude;
        double routeDy = destination.latitude - origin.latitude;
        double routeLength = Math.max(Math.sqrt(routeDx * routeDx + routeDy * routeDy), EPSILON);

        List<ZoneShape> crossedZoneShapes = fastestRoute.zonasRiesgo().stream()
                .map(zone -> {
                    ZonaRiesgo riskZone = zonesById.get(zone.getIdZona());
                    if (riskZone == null) {
                        throw new ApplicationConfigurationException(
                                "No se encontro la zona de riesgo asociada a la alternativa calculada");
                    }
                    return buildZoneShape(riskZone, origin, routeDx, routeDy, routeLength);
                })
                .sorted(Comparator.comparingDouble(ZoneShape::projection))
                .toList();

        List<SafeRouteGeometryDTO> candidates = new ArrayList<>();
        for (int direction : List.of(1, -1)) {
            for (int multiplier : List.of(1, 2, 3, 4)) {
                List<List<BigDecimal>> coordinates = new ArrayList<>();
                coordinates.add(toCoordinate(origin.longitude, origin.latitude));
                for (ZoneShape zoneShape : crossedZoneShapes) {
                    GeoPoint detourPoint = buildDetourPoint(
                            origin,
                            destination,
                            zoneShape,
                            direction,
                            multiplier);
                    appendCoordinateIfNeeded(coordinates, toCoordinate(detourPoint.longitude, detourPoint.latitude));
                }
                appendCoordinateIfNeeded(coordinates, toCoordinate(destination.longitude, destination.latitude));
                candidates.add(SafeRouteGeometryDTO.builder()
                        .type("LineString")
                        .coordinates(coordinates)
                        .build());
            }
        }
        return candidates;
    }

    private void appendCoordinateIfNeeded(List<List<BigDecimal>> coordinates, List<BigDecimal> candidate) {
        if (coordinates.isEmpty()) {
            coordinates.add(candidate);
            return;
        }

        List<BigDecimal> last = coordinates.get(coordinates.size() - 1);
        if (last.get(0).compareTo(candidate.get(0)) == 0 && last.get(1).compareTo(candidate.get(1)) == 0) {
            return;
        }
        coordinates.add(candidate);
    }

    private GeoPoint buildDetourPoint(
            GeoPoint origin,
            GeoPoint destination,
            ZoneShape zoneShape,
            int direction,
            int multiplier) {
        double routeDx = destination.longitude - origin.longitude;
        double routeDy = destination.latitude - origin.latitude;
        double routeLength = Math.max(Math.sqrt(routeDx * routeDx + routeDy * routeDy), EPSILON);
        double perpendicularX = -routeDy / routeLength;
        double perpendicularY = routeDx / routeLength;
        double baseOffset = Math.max(zoneShape.halfWidth(), zoneShape.halfHeight());
        double offset = baseOffset * (1.75d * multiplier) + COORDINATE_MARGIN.doubleValue();

        double longitude = zoneShape.centerLongitude() + (perpendicularX * direction * offset);
        double latitude = zoneShape.centerLatitude() + (perpendicularY * direction * offset);
        return new GeoPoint(
                clamp(longitude, -180d, 180d),
                clamp(latitude, -90d, 90d));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ZoneShape buildZoneShape(
            ZonaRiesgo zone,
            GeoPoint origin,
            double routeDx,
            double routeDy,
            double routeLength) {
        RiskZoneGeometryDTO geometry = deserializeRiskZoneGeometry(zone.getCoordenadasGeojson());
        if (!"Polygon".equalsIgnoreCase(geometry.getType())) {
            log.error("Zona de riesgo con geometria no soportada zoneId={} type={}",
                    zone.getIdZona(), geometry.getType());
            throw new ApplicationConfigurationException(
                    "La zona de riesgo contiene una geometria no soportada para el calculo de rutas");
        }
        if (geometry.getCoordinates() == null || geometry.getCoordinates().isEmpty()) {
            log.error("Zona de riesgo con anillos de poligono vacios zoneId={}", zone.getIdZona());
            throw new ApplicationConfigurationException(
                    "La zona de riesgo no contiene coordenadas suficientes para el calculo de rutas");
        }

        double minLongitude = Double.POSITIVE_INFINITY;
        double maxLongitude = Double.NEGATIVE_INFINITY;
        double minLatitude = Double.POSITIVE_INFINITY;
        double maxLatitude = Double.NEGATIVE_INFINITY;

        for (List<List<BigDecimal>> ringCoordinates : geometry.getCoordinates()) {
            validatePolygonRing(zone.getIdZona(), ringCoordinates);
            for (List<BigDecimal> coordinate : ringCoordinates) {
                GeoPoint point = toGeoPoint(coordinate);
                minLongitude = Math.min(minLongitude, point.longitude);
                maxLongitude = Math.max(maxLongitude, point.longitude);
                minLatitude = Math.min(minLatitude, point.latitude);
                maxLatitude = Math.max(maxLatitude, point.latitude);
            }
        }

        double centerLongitude = (minLongitude + maxLongitude) / 2d;
        double centerLatitude = (minLatitude + maxLatitude) / 2d;
        double projection = ((centerLongitude - origin.longitude) * routeDx
                + (centerLatitude - origin.latitude) * routeDy) / routeLength;

        return new ZoneShape(
                centerLongitude,
                centerLatitude,
                Math.max((maxLongitude - minLongitude) / 2d, COORDINATE_MARGIN.doubleValue()),
                Math.max((maxLatitude - minLatitude) / 2d, COORDINATE_MARGIN.doubleValue()),
                projection);
    }

    private RouteAlternative evaluateRoute(
            SafeRouteGeometryDTO geometry,
            List<ZonaRiesgo> activeZones,
            Map<Integer, Ubicacion> locationsById) {
        List<SafeRouteRiskZoneDTO> crossedZones = activeZones.stream()
                .filter(zone -> intersectsRoute(geometry, zone))
                .map(zone -> buildCrossedZone(zone, locationsById.get(zone.getUbicacion().getIdUbicacion())))
                .sorted(Comparator.comparing(SafeRouteRiskZoneDTO::getNivelRiesgo).reversed()
                        .thenComparing(SafeRouteRiskZoneDTO::getIdZona))
                .toList();

        int distanceMeters = calculateDistanceInMeters(geometry.getCoordinates());
        int estimatedTimeMinutes = Math.max(1, (int) Math.ceil(distanceMeters / WALKING_SPEED_METERS_PER_MINUTE));
        int riskScore = calculateRiskScore(crossedZones);
        int routeRiskLevel = resolveRiskLevelFromScore(riskScore);

        return new RouteAlternative(
                geometry,
                distanceMeters,
                estimatedTimeMinutes,
                riskScore,
                routeRiskLevel,
                crossedZones);
    }

    private int calculateRiskScore(List<SafeRouteRiskZoneDTO> crossedZones) {
        if (crossedZones.isEmpty()) {
            return LOW_RISK_SCORE;
        }

        int baseScore = crossedZones.stream()
                .mapToInt(zone -> switch (zone.getNivelRiesgo()) {
                    case LOW_RISK_LEVEL -> LOW_RISK_SCORE + 10;
                    case MEDIUM_RISK_LEVEL -> MEDIUM_RISK_SCORE + 10;
                    case HIGH_RISK_LEVEL -> HIGH_RISK_SCORE + 5;
                    default -> throw new ApplicationConfigurationException(
                            "La ruta contiene un nivel de riesgo no soportado");
                })
                .sum();
        return Math.min(100, baseScore + Math.max(0, crossedZones.size() - 1) * 5);
    }

    private int resolveRiskLevelFromScore(int riskScore) {
        if (riskScore <= 30) {
            return LOW_RISK_LEVEL;
        }
        if (riskScore <= 69) {
            return MEDIUM_RISK_LEVEL;
        }
        return HIGH_RISK_LEVEL;
    }

    private RouteAlternative selectRecommendedRoute(
            RouteAlternative fastestRoute,
            RouteAlternative safestRoute) {
        int fastestTradeoff = fastestRoute.scoreRiesgo();
        int safestTradeoff = safestRoute.scoreRiesgo()
                + Math.max(0, safestRoute.tiempoEstimado() - fastestRoute.tiempoEstimado()) * 4;

        if (safestTradeoff < fastestTradeoff) {
            return safestRoute;
        }
        if (safestTradeoff > fastestTradeoff) {
            return fastestRoute;
        }
        if (safestRoute.scoreRiesgo() < fastestRoute.scoreRiesgo()) {
            return safestRoute;
        }
        return fastestRoute;
    }

    private String buildRecommendation(
            RouteAlternative fastestRoute,
            RouteAlternative safestRoute,
            RouteAlternative recommendedRoute) {
        if (recommendedRoute.equals(safestRoute) && !recommendedRoute.equals(fastestRoute)) {
            int extraMinutes = Math.max(0, safestRoute.tiempoEstimado() - fastestRoute.tiempoEstimado());
            return "Se recomienda la ruta mas segura porque reduce el score de riesgo de "
                    + fastestRoute.scoreRiesgo()
                    + " a "
                    + safestRoute.scoreRiesgo()
                    + " con un aumento estimado de "
                    + extraMinutes
                    + " minutos.";
        }

        if (fastestRoute.zonasRiesgo().isEmpty()) {
            return "Se recomienda la ruta mas rapida porque no cruza zonas de riesgo activas.";
        }

        if (fastestRoute.scoreRiesgo() <= safestRoute.scoreRiesgo()) {
            return "Se recomienda la ruta mas rapida porque mantiene el menor tiempo estimado sin empeorar el riesgo.";
        }

        return "Se recomienda la ruta mas rapida porque el desvio alternativo no compensa el tiempo adicional.";
    }

    private Usuario resolveActiveAuthenticatedUser(String authenticatedEmail) {
        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Calculo de ruta rechazado por falta de sesion autenticada");
            throw new UnauthenticatedUserException("No existe una sesion autenticada para calcular la ruta");
        }

        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Usuario autenticado no encontrado durante calculo de ruta email={}", normalizedEmail);
                    return new ResourceNotFoundException("No se encontro el usuario autenticado");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Calculo de ruta rechazado por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        return usuario;
    }

    private SafeRouteRequestDTO normalizeAndValidateRequest(SafeRouteRequestDTO request) {
        if (request == null) {
            log.warn("Calculo de ruta rechazado porque la solicitud es nula");
            throw new InvalidSafeRouteRequestException("La solicitud de ruta es obligatoria");
        }
        if (request.getOrigen() == null) {
            log.warn("Calculo de ruta rechazado porque el origen es obligatorio");
            throw new InvalidSafeRouteRequestException("El origen es obligatorio");
        }
        if (request.getDestino() == null) {
            log.warn("Calculo de ruta rechazado porque el destino es obligatorio");
            throw new InvalidSafeRouteRequestException("El destino es obligatorio");
        }

        SafeRoutePointDTO origin = normalizePoint(request.getOrigen(), "origen");
        SafeRoutePointDTO destination = normalizePoint(request.getDestino(), "destino");

        if (origin.getLatitud().compareTo(destination.getLatitud()) == 0
                && origin.getLongitud().compareTo(destination.getLongitud()) == 0) {
            log.warn("Calculo de ruta rechazado porque origen y destino son iguales");
            throw new InvalidSafeRouteRequestException("El origen y el destino no pueden ser iguales");
        }

        return SafeRouteRequestDTO.builder()
                .origen(origin)
                .destino(destination)
                .build();
    }

    private SafeRoutePointDTO normalizePoint(SafeRoutePointDTO point, String pointName) {
        if (point.getLatitud() == null) {
            throw new InvalidSafeRouteRequestException("La latitud del " + pointName + " es obligatoria");
        }
        if (point.getLongitud() == null) {
            throw new InvalidSafeRouteRequestException("La longitud del " + pointName + " es obligatoria");
        }
        if (point.getLatitud().compareTo(new BigDecimal("-90")) < 0
                || point.getLatitud().compareTo(new BigDecimal("90")) > 0) {
            throw new InvalidSafeRouteRequestException("La latitud del " + pointName + " debe estar entre -90 y 90");
        }
        if (point.getLongitud().compareTo(new BigDecimal("-180")) < 0
                || point.getLongitud().compareTo(new BigDecimal("180")) > 0) {
            throw new InvalidSafeRouteRequestException("La longitud del " + pointName + " debe estar entre -180 y 180");
        }

        return SafeRoutePointDTO.builder()
                .latitud(point.getLatitud().setScale(7, RoundingMode.HALF_UP))
                .longitud(point.getLongitud().setScale(7, RoundingMode.HALF_UP))
                .referencia(normalizeOptionalText(point.getReferencia(), 150, "La referencia no puede superar los 150 caracteres"))
                .distrito(normalizeOptionalText(point.getDistrito(), 100, "El distrito no puede superar los 100 caracteres"))
                .ciudad(normalizeOptionalText(point.getCiudad(), 100, "La ciudad no puede superar los 100 caracteres"))
                .build();
    }

    private String normalizeOptionalText(String value, int maxLength, String validationMessage) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        if (!StringUtils.hasText(normalizedValue)) {
            return null;
        }
        if (normalizedValue.length() > maxLength) {
            throw new InvalidSafeRouteRequestException(validationMessage);
        }
        return normalizedValue;
    }

    private void validateGeographicDataAvailability() {
        if (ubicacionRepository.count() == 0) {
            log.warn("Calculo de ruta rechazado porque no existen datos geograficos");
            throw new GeographicDataNotAvailableException(
                    "No existen datos geograficos disponibles para calcular la ruta");
        }
    }

    private SafeRouteGeometryDTO buildDirectGeometry(SafeRouteRequestDTO request) {
        return SafeRouteGeometryDTO.builder()
                .type("LineString")
                .coordinates(List.of(
                        List.of(request.getOrigen().getLongitud(), request.getOrigen().getLatitud()),
                        List.of(request.getDestino().getLongitud(), request.getDestino().getLatitud())))
                .build();
    }

    private int calculateDistanceInMeters(List<List<BigDecimal>> coordinates) {
        int totalDistance = 0;
        List<GeoPoint> points = coordinates.stream()
                .map(this::toGeoPoint)
                .toList();

        for (int index = 0; index < points.size() - 1; index++) {
            totalDistance += calculateDistanceBetween(points.get(index), points.get(index + 1));
        }
        return totalDistance;
    }

    private int calculateDistanceBetween(GeoPoint origin, GeoPoint destination) {
        double lat1 = Math.toRadians(origin.latitude);
        double lon1 = Math.toRadians(origin.longitude);
        double lat2 = Math.toRadians(destination.latitude);
        double lon2 = Math.toRadians(destination.longitude);

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = Math.pow(Math.sin(deltaLat / 2d), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2d), 2);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));

        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }

    private Map<Integer, Ubicacion> loadLocationsById(List<ZonaRiesgo> activeZones) {
        List<Integer> locationIds = activeZones.stream()
                .map(zone -> {
                    Ubicacion ubicacion = zone.getUbicacion();
                    if (ubicacion == null) {
                        log.error("Inconsistencia de datos: ubicacion faltante para zoneId={}", zone.getIdZona());
                        throw new ApplicationConfigurationException(
                                "No se encontro la ubicacion asociada a una zona de riesgo activa");
                    }
                    return ubicacion.getIdUbicacion();
                })
                .distinct()
                .toList();

        Map<Integer, Ubicacion> locationsById = new HashMap<>();
        ubicacionRepository.findAllById(locationIds)
                .forEach(location -> locationsById.put(location.getIdUbicacion(), location));
        return locationsById;
    }

    private boolean intersectsRoute(SafeRouteGeometryDTO routeGeometry, ZonaRiesgo zone) {
        RiskZoneGeometryDTO zoneGeometry = deserializeRiskZoneGeometry(zone.getCoordenadasGeojson());
        if (!"Polygon".equalsIgnoreCase(zoneGeometry.getType())) {
            log.error("Zona de riesgo con geometria no soportada zoneId={} type={}",
                    zone.getIdZona(), zoneGeometry.getType());
            throw new ApplicationConfigurationException(
                    "La zona de riesgo contiene una geometria no soportada para el calculo de rutas");
        }
        if (zoneGeometry.getCoordinates() == null || zoneGeometry.getCoordinates().isEmpty()) {
            log.error("Zona de riesgo con anillos de poligono vacios zoneId={}", zone.getIdZona());
            throw new ApplicationConfigurationException(
                    "La zona de riesgo no contiene coordenadas suficientes para el calculo de rutas");
        }

        List<GeoPoint> routePoints = routeGeometry.getCoordinates().stream()
                .map(this::toGeoPoint)
                .toList();

        for (List<List<BigDecimal>> ringCoordinates : zoneGeometry.getCoordinates()) {
            validatePolygonRing(zone.getIdZona(), ringCoordinates);
            List<GeoPoint> polygon = ringCoordinates.stream()
                    .map(this::toGeoPoint)
                    .toList();

            for (GeoPoint routePoint : routePoints) {
                if (pointInsidePolygon(routePoint, polygon)) {
                    return true;
                }
            }

            for (int routeIndex = 0; routeIndex < routePoints.size() - 1; routeIndex++) {
                for (int polygonIndex = 0; polygonIndex < polygon.size() - 1; polygonIndex++) {
                    if (segmentsIntersect(
                            routePoints.get(routeIndex),
                            routePoints.get(routeIndex + 1),
                            polygon.get(polygonIndex),
                            polygon.get(polygonIndex + 1))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void validatePolygonRing(Integer zoneId, List<List<BigDecimal>> ringCoordinates) {
        if (ringCoordinates == null || ringCoordinates.size() < 4) {
            log.error("Zona de riesgo con poligono incompleto zoneId={} totalPuntos={}",
                    zoneId, ringCoordinates != null ? ringCoordinates.size() : 0);
            throw new ApplicationConfigurationException(
                    "La zona de riesgo no contiene coordenadas suficientes para el calculo de rutas");
        }
    }

    private SafeRouteRiskZoneDTO buildCrossedZone(ZonaRiesgo zone, Ubicacion location) {
        if (location == null) {
            log.error("Inconsistencia de datos: ubicacion faltante para zoneId={}", zone.getIdZona());
            throw new ApplicationConfigurationException(
                    "No se encontro la ubicacion asociada a una zona de riesgo activa");
        }

        return SafeRouteRiskZoneDTO.builder()
                .idZona(zone.getIdZona())
                .tipo(zone.getTipo())
                .nivelRiesgo(zone.getNivelRiesgo())
                .nivelRiesgoNombre(resolveRiskLevelName(zone.getNivelRiesgo()))
                .color(resolveRiskLevelColor(zone.getNivelRiesgo()))
                .descripcion(zone.getDescripcion())
                .centro(RiskZoneLocationDTO.builder()
                        .latitud(location.getLatitud())
                        .longitud(location.getLongitud())
                        .distrito(location.getDistrito())
                        .ciudad(location.getCiudad())
                        .build())
                .build();
    }

    private Ruta persistRoute(
            Usuario usuario,
            SafeRouteRequestDTO request,
            RouteAlternative recommendedRoute,
            SafeRouteResponseDTO response,
            List<ZonaRiesgo> activeZones) {
        Ruta route = Ruta.builder()
                .origenLatitud(request.getOrigen().getLatitud())
                .origenLongitud(request.getOrigen().getLongitud())
                .destinoLatitud(request.getDestino().getLatitud())
                .destinoLongitud(request.getDestino().getLongitud())
                .nivelRiesgo(recommendedRoute.nivelRiesgo())
                .distanciaMetros(recommendedRoute.distancia())
                .tiempoEstimadoMinutos(recommendedRoute.tiempoEstimado())
                .geometriaGeojson(serializeRouteGeometry(recommendedRoute.geometria()))
                .resultadoJson(serializeRouteResponse(response))
                .fechaCalculo(LocalDateTime.now())
                .usuario(usuario)
                .build();

        Ruta savedRoute = rutaRepository.save(route);
        if (!recommendedRoute.zonasRiesgo().isEmpty()) {
            List<RutaZona> routeZones = recommendedRoute.zonasRiesgo().stream()
                    .map(zone -> RutaZona.builder()
                            .id(new RutaZonaId(savedRoute.getIdRuta(), zone.getIdZona()))
                            .ruta(savedRoute)
                            .zona(findZoneById(activeZones, zone.getIdZona()))
                            .build())
                    .toList();
            rutaZonaRepository.saveAll(routeZones);
        }
        return savedRoute;
    }

    private ZonaRiesgo findZoneById(List<ZonaRiesgo> activeZones, Integer zoneId) {
        return activeZones.stream()
                .filter(zone -> zone.getIdZona().equals(zoneId))
                .findFirst()
                .orElseThrow(() -> new ApplicationConfigurationException(
                        "No se encontro la zona de riesgo asociada a la ruta calculada"));
    }

    private RiskZoneGeometryDTO deserializeRiskZoneGeometry(String geometryJson) {
        try {
            return objectMapper.readValue(geometryJson, RiskZoneGeometryDTO.class);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo deserializar la geometria almacenada de la zona de riesgo", exception);
            throw new ApplicationConfigurationException(
                    "No se pudo deserializar la geometria de la zona de riesgo");
        }
    }

    private String serializeRouteGeometry(SafeRouteGeometryDTO geometry) {
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo serializar la geometria de la ruta calculada", exception);
            throw new ApplicationConfigurationException(
                    "No se pudo serializar la geometria de la ruta calculada");
        }
    }

    private String serializeRouteResponse(SafeRouteResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo serializar el resultado de la ruta calculada", exception);
            throw new ApplicationConfigurationException(
                    "No se pudo serializar el resultado de la ruta calculada");
        }
    }

    private String resolveRiskLevelName(Integer riskLevel) {
        return switch (riskLevel) {
            case LOW_RISK_LEVEL -> "bajo";
            case MEDIUM_RISK_LEVEL -> "medio";
            case HIGH_RISK_LEVEL -> "alto";
            default -> throw new ApplicationConfigurationException("La ruta contiene un nivel de riesgo no soportado");
        };
    }

    private String resolveRiskLevelColor(Integer riskLevel) {
        return switch (riskLevel) {
            case LOW_RISK_LEVEL -> "#22C55E";
            case MEDIUM_RISK_LEVEL -> "#F59E0B";
            case HIGH_RISK_LEVEL -> "#DC2626";
            default -> throw new ApplicationConfigurationException("La ruta contiene un nivel de riesgo no soportado");
        };
    }

    private GeoPoint toGeoPoint(List<BigDecimal> coordinate) {
        if (coordinate == null || coordinate.size() < 2) {
            log.error("Zona de riesgo con coordenada incompleta coordinate={}", coordinate);
            throw new ApplicationConfigurationException(
                    "La zona de riesgo contiene coordenadas incompletas para el calculo de rutas");
        }
        return new GeoPoint(coordinate.get(0).doubleValue(), coordinate.get(1).doubleValue());
    }

    private List<BigDecimal> toCoordinate(double longitude, double latitude) {
        return List.of(
                BigDecimal.valueOf(longitude).setScale(7, RoundingMode.HALF_UP),
                BigDecimal.valueOf(latitude).setScale(7, RoundingMode.HALF_UP));
    }

    private boolean pointInsidePolygon(GeoPoint point, List<GeoPoint> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            GeoPoint current = polygon.get(i);
            GeoPoint previous = polygon.get(j);

            if (pointOnSegment(previous, point, current)) {
                return true;
            }

            boolean intersects = ((current.latitude > point.latitude) != (previous.latitude > point.latitude))
                    && (point.longitude < (previous.longitude - current.longitude)
                    * (point.latitude - current.latitude)
                    / (previous.latitude - current.latitude + EPSILON)
                    + current.longitude);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean segmentsIntersect(GeoPoint p1, GeoPoint q1, GeoPoint p2, GeoPoint q2) {
        int orientation1 = orientation(p1, q1, p2);
        int orientation2 = orientation(p1, q1, q2);
        int orientation3 = orientation(p2, q2, p1);
        int orientation4 = orientation(p2, q2, q1);

        if (orientation1 != orientation2 && orientation3 != orientation4) {
            return true;
        }
        if (orientation1 == 0 && pointOnSegment(p1, p2, q1)) {
            return true;
        }
        if (orientation2 == 0 && pointOnSegment(p1, q2, q1)) {
            return true;
        }
        if (orientation3 == 0 && pointOnSegment(p2, p1, q2)) {
            return true;
        }
        return orientation4 == 0 && pointOnSegment(p2, q1, q2);
    }

    private int orientation(GeoPoint p, GeoPoint q, GeoPoint r) {
        double value = (q.latitude - p.latitude) * (r.longitude - q.longitude)
                - (q.longitude - p.longitude) * (r.latitude - q.latitude);
        if (Math.abs(value) < EPSILON) {
            return 0;
        }
        return value > 0 ? 1 : 2;
    }

    private boolean pointOnSegment(GeoPoint p, GeoPoint q, GeoPoint r) {
        return q.longitude <= Math.max(p.longitude, r.longitude) + EPSILON
                && q.longitude + EPSILON >= Math.min(p.longitude, r.longitude)
                && q.latitude <= Math.max(p.latitude, r.latitude) + EPSILON
                && q.latitude + EPSILON >= Math.min(p.latitude, r.latitude)
                && Math.abs((r.longitude - p.longitude) * (q.latitude - p.latitude)
                - (r.latitude - p.latitude) * (q.longitude - p.longitude)) < EPSILON;
    }

    private record GeoPoint(double longitude, double latitude) {
    }

    private record ZoneShape(
            double centerLongitude,
            double centerLatitude,
            double halfWidth,
            double halfHeight,
            double projection) {
    }

    private record RouteAlternative(
            SafeRouteGeometryDTO geometria,
            int distancia,
            int tiempoEstimado,
            int scoreRiesgo,
            int nivelRiesgo,
            List<SafeRouteRiskZoneDTO> zonasRiesgo) {
    }
}
