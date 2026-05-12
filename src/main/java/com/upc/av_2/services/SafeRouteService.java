package com.upc.av_2.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.av_2.dtos.RiskZoneGeometryDTO;
import com.upc.av_2.dtos.RiskZoneLocationDTO;
import com.upc.av_2.dtos.SafeRouteGeometryDTO;
import com.upc.av_2.dtos.SafeRoutePointDTO;
import com.upc.av_2.dtos.SafeRouteRequestDTO;
import com.upc.av_2.dtos.SafeRouteResponseDTO;
import com.upc.av_2.dtos.SafeRouteRiskZoneDTO;
import com.upc.av_2.entidades.Ruta;
import com.upc.av_2.entidades.RutaZona;
import com.upc.av_2.entidades.RutaZonaId;
import com.upc.av_2.entidades.Ubicacion;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.entidades.ZonaRiesgo;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.GeographicDataNotAvailableException;
import com.upc.av_2.exceptions.InvalidSafeRouteRequestException;
import com.upc.av_2.exceptions.ResourceNotFoundException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.RutaRepository;
import com.upc.av_2.repositories.RutaZonaRepository;
import com.upc.av_2.repositories.UbicacionRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.repositories.ZonaRiesgoRepository;
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
    private static final double WALKING_SPEED_METERS_PER_MINUTE = 83.3333333333d;
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final double EPSILON = 1e-9d;

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

        SafeRouteGeometryDTO geometry = buildRouteGeometry(normalizedRequest);
        int distanceMeters = calculateDistanceInMeters(
                normalizedRequest.getOrigen(),
                normalizedRequest.getDestino());
        int estimatedTimeMinutes = Math.max(1, (int) Math.ceil(distanceMeters / WALKING_SPEED_METERS_PER_MINUTE));

        List<ZonaRiesgo> activeZones = zonaRiesgoRepository
                .findByEstadoTrueOrderByNivelRiesgoDescFechaActualizacionDesc();
        Map<Integer, Ubicacion> locationsById = loadLocationsById(activeZones);

        List<SafeRouteRiskZoneDTO> crossedZones = activeZones.stream()
                .filter(zone -> intersectsRoute(geometry, zone))
                .map(zone -> buildCrossedZone(zone, locationsById.get(zone.getUbicacionIdUbicacion())))
                .sorted(Comparator.comparing(SafeRouteRiskZoneDTO::getNivelRiesgo).reversed()
                        .thenComparing(SafeRouteRiskZoneDTO::getIdZona))
                .toList();

        int routeRiskLevel = crossedZones.stream()
                .map(SafeRouteRiskZoneDTO::getNivelRiesgo)
                .max(Integer::compareTo)
                .orElse(LOW_RISK_LEVEL);

        Ruta savedRoute = persistRoute(
                usuario,
                normalizedRequest,
                geometry,
                distanceMeters,
                estimatedTimeMinutes,
                routeRiskLevel,
                crossedZones);

        log.info("Ruta segura calculada email={} rutaId={} distancia={} riesgo={} zonas={}",
                authenticatedEmail,
                savedRoute.getIdRuta(),
                distanceMeters,
                routeRiskLevel,
                crossedZones.size());

        return SafeRouteResponseDTO.builder()
                .rutaId(savedRoute.getIdRuta())
                .mensaje("Ruta segura calculada correctamente")
                .origen(normalizedRequest.getOrigen())
                .destino(normalizedRequest.getDestino())
                .distanciaMetros(distanceMeters)
                .tiempoEstimadoMinutos(estimatedTimeMinutes)
                .nivelRiesgo(routeRiskLevel)
                .nivelRiesgoNombre(resolveRiskLevelName(routeRiskLevel))
                .cruzaZonasRiesgo(!crossedZones.isEmpty())
                .recomendaciones(buildRecommendations(routeRiskLevel, crossedZones.size()))
                .geometria(geometry)
                .zonasRiesgo(crossedZones)
                .build();
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

    private SafeRouteGeometryDTO buildRouteGeometry(SafeRouteRequestDTO request) {
        return SafeRouteGeometryDTO.builder()
                .type("LineString")
                .coordinates(List.of(
                        List.of(request.getOrigen().getLongitud(), request.getOrigen().getLatitud()),
                        List.of(request.getDestino().getLongitud(), request.getDestino().getLatitud())))
                .build();
    }

    private int calculateDistanceInMeters(SafeRoutePointDTO origin, SafeRoutePointDTO destination) {
        double lat1 = Math.toRadians(origin.getLatitud().doubleValue());
        double lon1 = Math.toRadians(origin.getLongitud().doubleValue());
        double lat2 = Math.toRadians(destination.getLatitud().doubleValue());
        double lon2 = Math.toRadians(destination.getLongitud().doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = Math.pow(Math.sin(deltaLat / 2d), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2d), 2);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));

        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }

    private Map<Integer, Ubicacion> loadLocationsById(List<ZonaRiesgo> activeZones) {
        List<Integer> locationIds = activeZones.stream()
                .map(ZonaRiesgo::getUbicacionIdUbicacion)
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

        GeoPoint origin = toGeoPoint(routeGeometry.getCoordinates().get(0));
        GeoPoint destination = toGeoPoint(routeGeometry.getCoordinates().get(1));

        for (List<List<BigDecimal>> ringCoordinates : zoneGeometry.getCoordinates()) {
            validatePolygonRing(zone.getIdZona(), ringCoordinates);
            List<GeoPoint> polygon = ringCoordinates.stream()
                    .map(this::toGeoPoint)
                    .toList();

            if (pointInsidePolygon(origin, polygon) || pointInsidePolygon(destination, polygon)) {
                return true;
            }

            for (int index = 0; index < polygon.size() - 1; index++) {
                if (segmentsIntersect(origin, destination, polygon.get(index), polygon.get(index + 1))) {
                    return true;
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
            SafeRouteGeometryDTO geometry,
            int distanceMeters,
            int estimatedTimeMinutes,
            int routeRiskLevel,
            List<SafeRouteRiskZoneDTO> crossedZones) {
        Ruta route = Ruta.builder()
                .origenLatitud(request.getOrigen().getLatitud())
                .origenLongitud(request.getOrigen().getLongitud())
                .destinoLatitud(request.getDestino().getLatitud())
                .destinoLongitud(request.getDestino().getLongitud())
                .nivelRiesgo(routeRiskLevel)
                .distanciaMetros(distanceMeters)
                .tiempoEstimadoMinutos(estimatedTimeMinutes)
                .geometriaGeojson(serializeRouteGeometry(geometry))
                .fechaCalculo(LocalDateTime.now())
                .usuarioIdUsuario(usuario.getIdUsuario())
                .build();

        Ruta savedRoute = rutaRepository.save(route);
        if (!crossedZones.isEmpty()) {
            List<RutaZona> routeZones = crossedZones.stream()
                    .map(zone -> RutaZona.builder()
                            .id(new RutaZonaId(savedRoute.getIdRuta(), zone.getIdZona()))
                            .build())
                    .toList();
            rutaZonaRepository.saveAll(routeZones);
        }
        return savedRoute;
    }

    private List<String> buildRecommendations(int routeRiskLevel, int crossedZonesCount) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Mantenerse atento al entorno durante todo el trayecto");

        if (crossedZonesCount > 0) {
            recommendations.add("Priorizar vias principales y zonas con mayor iluminacion");
        }
        if (routeRiskLevel >= MEDIUM_RISK_LEVEL) {
            recommendations.add("Evitar detenerse en tramos con menor flujo peatonal");
        }
        if (routeRiskLevel >= HIGH_RISK_LEVEL) {
            recommendations.add("Considerar una ruta alternativa o realizar el trayecto acompanado");
        }
        return recommendations;
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
}
