package com.upc.grupo3.services;

import com.upc.grupo3.dtos.routeevaluation.GeoJsonLineStringDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteOptionDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteRiskZoneDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteStepDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RouteAlternativeSyntheticService {

    private static final int MINIMUM_ALTERNATIVES = 3;
    private static final double MIN_OFFSET_DEGREES = 0.0009d;
    private static final double MAX_OFFSET_DEGREES = 0.0030d;
    private static final double EPSILON = 1e-9d;
    private static final Pattern GENERIC_ROUTE_SUMMARY = Pattern.compile("^ruta\\s+\\d+$", Pattern.CASE_INSENSITIVE);

    private static final List<RiskProfile> RISK_PROFILES = List.of(
            new RiskProfile(
                    "Ruta directa",
                    "alto",
                    82,
                    List.of(
                            zone(9101, "ROBO", 3, "Alto", "#DC2626",
                                    "Tramo sintetico con reportes recientes en horario pico."),
                            zone(9102, "ALERTA CIUDADANA", 2, "Medio", "#F59E0B",
                                    "Cruce sintetico con exposicion media y menor iluminacion.")),
                    1.00d,
                    1.00d,
                    0d),
            new RiskProfile(
                    "Ruta balanceada",
                    "medio",
                    46,
                    List.of(
                            zone(9201, "ALERTA CIUDADANA", 2, "Medio", "#F59E0B",
                                    "Desvio sintetico con un unico tramo de vigilancia media.")),
                    1.08d,
                    1.10d,
                    1.00d),
            new RiskProfile(
                    "Ruta de menor riesgo",
                    "bajo",
                    18,
                    List.of(),
                    1.16d,
                    1.18d,
                    -1.45d));

    public List<RouteOptionDTO> enrichRoutes(List<RouteOptionDTO> mapboxRoutes) {
        List<PreparedRoute> preparedRoutes = ensureMinimumAlternatives(mapboxRoutes);
        List<RouteOptionDTO> enrichedRoutes = new ArrayList<>();

        for (int index = 0; index < preparedRoutes.size(); index++) {
            PreparedRoute preparedRoute = preparedRoutes.get(index);
            RiskProfile riskProfile = riskProfileFor(index);
            RouteOptionDTO baseRoute = preparedRoute.route();
            List<RouteRiskZoneDTO> zones = copyZones(riskProfile.zones());

            enrichedRoutes.add(RouteOptionDTO.builder()
                    .routeId(resolveRouteId(baseRoute, index))
                    .summary(resolveSummary(baseRoute, riskProfile, preparedRoute.synthetic()))
                    .durationMinutes(defaultMetric(baseRoute.getDurationMinutes(), 0d))
                    .distanceKm(defaultMetric(baseRoute.getDistanceKm(), 0d))
                    .geometry(copyGeometry(baseRoute.getGeometry()))
                    .steps(resolveSteps(baseRoute, riskProfile, preparedRoute.synthetic()))
                    .nivelRiesgo(riskProfile.riskLevel())
                    .scoreRiesgo(riskProfile.riskScore())
                    .cruzaZonasRiesgo(!zones.isEmpty())
                    .zonasRiesgo(zones)
                    .recomendada(false)
                    .build());
        }

        String recommendedRouteId = selectRecommendedRouteId(enrichedRoutes);
        return enrichedRoutes.stream()
                .map(route -> route.toBuilder()
                        .recomendada(route.getRouteId().equals(recommendedRouteId))
                        .build())
                .toList();
    }

    private List<PreparedRoute> ensureMinimumAlternatives(List<RouteOptionDTO> mapboxRoutes) {
        List<PreparedRoute> preparedRoutes = new ArrayList<>();
        for (RouteOptionDTO route : mapboxRoutes) {
            preparedRoutes.add(new PreparedRoute(route, false));
        }

        RouteOptionDTO baseRoute = mapboxRoutes.get(0);
        int syntheticIndex = 0;
        while (preparedRoutes.size() < MINIMUM_ALTERNATIVES) {
            int routeIndex = preparedRoutes.size();
            preparedRoutes.add(new PreparedRoute(
                    buildSyntheticRoute(baseRoute, routeIndex, riskProfileFor(routeIndex), syntheticIndex),
                    true));
            syntheticIndex++;
        }
        return preparedRoutes;
    }

    private RouteOptionDTO buildSyntheticRoute(
            RouteOptionDTO baseRoute,
            int routeIndex,
            RiskProfile riskProfile,
            int syntheticIndex) {
        return RouteOptionDTO.builder()
                .routeId("route_" + (routeIndex + 1))
                .summary(riskProfile.summary())
                .durationMinutes(scaleMetric(baseRoute.getDurationMinutes(), riskProfile.durationMultiplier()))
                .distanceKm(scaleMetric(baseRoute.getDistanceKm(), riskProfile.distanceMultiplier()))
                .geometry(buildShiftedGeometry(baseRoute.getGeometry(), riskProfile.geometryMultiplier(), syntheticIndex))
                .steps(buildSyntheticSteps(baseRoute.getSteps(), riskProfile))
                .build();
    }

    private GeoJsonLineStringDTO buildShiftedGeometry(
            GeoJsonLineStringDTO baseGeometry,
            double geometryMultiplier,
            int syntheticIndex) {
        GeoJsonLineStringDTO safeGeometry = copyGeometry(baseGeometry);
        List<List<Double>> coordinates = safeGeometry.getCoordinates();
        if (coordinates == null || coordinates.size() < 2 || Math.abs(geometryMultiplier) < EPSILON) {
            return safeGeometry;
        }

        double amplitude = clamp(
                straightLineLength(coordinates) * 0.03d * Math.abs(geometryMultiplier),
                MIN_OFFSET_DEGREES,
                MAX_OFFSET_DEGREES);
        double direction = geometryMultiplier >= 0 ? 1d : -1d;
        double waveFactor = syntheticIndex % 2 == 0 ? 1d : 1.15d;

        List<List<Double>> shiftedCoordinates = new ArrayList<>(coordinates.size());
        for (int index = 0; index < coordinates.size(); index++) {
            List<Double> coordinate = coordinates.get(index);
            if (coordinate == null || coordinate.size() < 2) {
                shiftedCoordinates.add(copyCoordinate(coordinate));
                continue;
            }

            if (index == 0 || index == coordinates.size() - 1) {
                shiftedCoordinates.add(copyCoordinate(coordinate));
                continue;
            }

            List<Double> previous = coordinates.get(index - 1);
            List<Double> next = coordinates.get(index + 1);
            double tangentLongitude = next.get(0) - previous.get(0);
            double tangentLatitude = next.get(1) - previous.get(1);
            double tangentLength = Math.max(Math.sqrt(
                    tangentLongitude * tangentLongitude + tangentLatitude * tangentLatitude), EPSILON);

            double progress = (double) index / (coordinates.size() - 1);
            double weight = Math.sin(Math.PI * progress);
            double offset = amplitude * weight * waveFactor * direction;
            double perpendicularLongitude = -tangentLatitude / tangentLength;
            double perpendicularLatitude = tangentLongitude / tangentLength;

            shiftedCoordinates.add(List.of(
                    roundCoordinate(coordinate.get(0) + perpendicularLongitude * offset),
                    roundCoordinate(coordinate.get(1) + perpendicularLatitude * offset)));
        }

        return GeoJsonLineStringDTO.builder()
                .type(StringUtils.hasText(safeGeometry.getType()) ? safeGeometry.getType() : "LineString")
                .coordinates(shiftedCoordinates)
                .build();
    }

    private List<RouteStepDTO> resolveSteps(
            RouteOptionDTO route,
            RiskProfile riskProfile,
            boolean synthetic) {
        if (!synthetic) {
            return copySteps(route.getSteps());
        }
        return buildSyntheticSteps(route.getSteps(), riskProfile);
    }

    private List<RouteStepDTO> buildSyntheticSteps(
            List<RouteStepDTO> baseSteps,
            RiskProfile riskProfile) {
        if (baseSteps == null || baseSteps.isEmpty()) {
            return List.of(
                    step(1, "Salida", 400d, 120d,
                            "Inicia por una variante " + riskProfile.summary().toLowerCase() + "."),
                    step(2, "Corredor principal", 900d, 240d,
                            "Mantente en el corredor alternativo para continuar el trayecto."),
                    step(3, "Destino", 0d, 0d, "Has llegado al destino."));
        }

        List<RouteStepDTO> copiedSteps = new ArrayList<>(copySteps(baseSteps));
        RouteStepDTO firstStep = copiedSteps.get(0);
        copiedSteps.set(0, RouteStepDTO.builder()
                .order(firstStep.getOrder())
                .instruction(instructionForProfile(riskProfile))
                .streetName(firstStep.getStreetName())
                .distanceMeters(firstStep.getDistanceMeters())
                .durationSeconds(firstStep.getDurationSeconds())
                .maneuverType(firstStep.getManeuverType())
                .modifier(firstStep.getModifier())
                .build());
        return copiedSteps;
    }

    private String instructionForProfile(RiskProfile riskProfile) {
        return switch (riskProfile.riskLevel()) {
            case "alto" -> "Avanza por el tramo directo con mayor exposicion sintetica.";
            case "medio" -> "Toma el desvio balanceado para reducir parte de la exposicion.";
            default -> "Continua por la variante de menor riesgo y mejor iluminacion sintetica.";
        };
    }

    private String resolveRouteId(RouteOptionDTO route, int index) {
        return StringUtils.hasText(route.getRouteId()) ? route.getRouteId() : "route_" + (index + 1);
    }

    private String resolveSummary(
            RouteOptionDTO route,
            RiskProfile riskProfile,
            boolean synthetic) {
        if (synthetic) {
            return riskProfile.summary();
        }

        String summary = route.getSummary();
        if (!StringUtils.hasText(summary) || GENERIC_ROUTE_SUMMARY.matcher(summary.trim()).matches()) {
            return riskProfile.summary();
        }

        return summary;
    }

    private String selectRecommendedRouteId(List<RouteOptionDTO> routes) {
        double fastestDuration = routes.stream()
                .map(RouteOptionDTO::getDurationMinutes)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(0d);

        Comparator<RouteOptionDTO> recommendationComparator = Comparator
                .comparingDouble((RouteOptionDTO route) -> recommendationScore(route, fastestDuration))
                .thenComparing(RouteOptionDTO::getRouteId);

        return routes.stream()
                .min(recommendationComparator)
                .map(RouteOptionDTO::getRouteId)
                .orElse(routes.get(0).getRouteId());
    }

    private double recommendationScore(RouteOptionDTO route, double fastestDuration) {
        double riskScore = route.getScoreRiesgo() != null ? route.getScoreRiesgo() : 100d;
        double durationPenalty = route.getDurationMinutes() != null
                ? Math.max(0d, route.getDurationMinutes() - fastestDuration) * 6d
                : 0d;
        return riskScore + durationPenalty;
    }

    private RiskProfile riskProfileFor(int index) {
        if (index < RISK_PROFILES.size()) {
            return RISK_PROFILES.get(index);
        }
        return index % 2 == 0 ? RISK_PROFILES.get(1) : RISK_PROFILES.get(2);
    }

    private List<RouteRiskZoneDTO> copyZones(List<RouteRiskZoneDTO> zones) {
        return zones.stream()
                .map(zone -> RouteRiskZoneDTO.builder()
                        .idZona(zone.getIdZona())
                        .tipo(zone.getTipo())
                        .nivelRiesgo(zone.getNivelRiesgo())
                        .nivelRiesgoNombre(zone.getNivelRiesgoNombre())
                        .color(zone.getColor())
                        .descripcion(zone.getDescripcion())
                        .build())
                .toList();
    }

    private GeoJsonLineStringDTO copyGeometry(GeoJsonLineStringDTO geometry) {
        if (geometry == null) {
            return GeoJsonLineStringDTO.builder()
                    .type("LineString")
                    .coordinates(List.of())
                    .build();
        }

        List<List<Double>> coordinates = geometry.getCoordinates() == null
                ? List.of()
                : geometry.getCoordinates().stream()
                        .map(this::copyCoordinate)
                        .toList();

        return GeoJsonLineStringDTO.builder()
                .type(StringUtils.hasText(geometry.getType()) ? geometry.getType() : "LineString")
                .coordinates(coordinates)
                .build();
    }

    private List<RouteStepDTO> copySteps(List<RouteStepDTO> steps) {
        if (steps == null) {
            return List.of();
        }

        return steps.stream()
                .map(step -> RouteStepDTO.builder()
                        .order(step.getOrder())
                        .instruction(step.getInstruction())
                        .streetName(step.getStreetName())
                        .distanceMeters(step.getDistanceMeters())
                        .durationSeconds(step.getDurationSeconds())
                        .maneuverType(step.getManeuverType())
                        .modifier(step.getModifier())
                        .build())
                .toList();
    }

    private Double scaleMetric(Double value, double multiplier) {
        double safeValue = defaultMetric(value, 0d);
        return safeValue == 0d ? 0d : roundMetric(safeValue * multiplier);
    }

    private Double defaultMetric(Double value, double fallback) {
        return value != null && !value.isNaN() ? value : fallback;
    }

    private double straightLineLength(List<List<Double>> coordinates) {
        List<Double> origin = coordinates.get(0);
        List<Double> destination = coordinates.get(coordinates.size() - 1);
        double deltaLongitude = destination.get(0) - origin.get(0);
        double deltaLatitude = destination.get(1) - origin.get(1);
        return Math.sqrt(deltaLongitude * deltaLongitude + deltaLatitude * deltaLatitude);
    }

    private List<Double> copyCoordinate(List<Double> coordinate) {
        if (coordinate == null || coordinate.size() < 2) {
            return List.of();
        }
        return List.of(coordinate.get(0), coordinate.get(1));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Double roundMetric(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double roundCoordinate(double value) {
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static RouteRiskZoneDTO zone(
            int idZona,
            String tipo,
            int nivelRiesgo,
            String nivelRiesgoNombre,
            String color,
            String descripcion) {
        return RouteRiskZoneDTO.builder()
                .idZona(idZona)
                .tipo(tipo)
                .nivelRiesgo(nivelRiesgo)
                .nivelRiesgoNombre(nivelRiesgoNombre)
                .color(color)
                .descripcion(descripcion)
                .build();
    }

    private static RouteStepDTO step(
            int order,
            String streetName,
            double distanceMeters,
            double durationSeconds,
            String instruction) {
        return RouteStepDTO.builder()
                .order(order)
                .instruction(instruction)
                .streetName(streetName)
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .maneuverType(order == 1 ? "depart" : order == 3 ? "arrive" : "turn")
                .modifier(null)
                .build();
    }

    private record PreparedRoute(RouteOptionDTO route, boolean synthetic) {
    }

    private record RiskProfile(
            String summary,
            String riskLevel,
            Integer riskScore,
            List<RouteRiskZoneDTO> zones,
            double durationMultiplier,
            double distanceMultiplier,
            double geometryMultiplier) {
    }
}