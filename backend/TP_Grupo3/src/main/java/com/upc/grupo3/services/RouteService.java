package com.upc.grupo3.services;

import com.upc.grupo3.clients.MapboxClient;
import com.upc.grupo3.dtos.routeevaluation.ResolvedPlaceDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateRequestDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateResponseDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteOptionDTO;
import com.upc.grupo3.exceptions.InvalidRouteRequestException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {

    private static final String DEFAULT_TRANSPORT_MODE = "driving";
    private static final Set<String> ALLOWED_TRANSPORT_MODES = Set.of("driving", "walking", "cycling");

    private final MapboxClient mapboxClient;

    public RouteEvaluateResponseDTO evaluateRoute(RouteEvaluateRequestDTO request) {
        NormalizedRequest normalizedRequest = normalizeAndValidate(request);

        log.info("Evaluando rutas origin='{}' destination='{}' mode={}",
                normalizedRequest.origin(),
                normalizedRequest.destination(),
                normalizedRequest.transportMode());

        ResolvedPlaceDTO originResolved = mapboxClient.geocode(normalizedRequest.origin(), "origen");
        ResolvedPlaceDTO destinationResolved = mapboxClient.geocode(normalizedRequest.destination(), "destino");

        validateResolvedLocations(originResolved, destinationResolved);

        List<RouteOptionDTO> routes = mapboxClient.getDirections(
                originResolved,
                destinationResolved,
                normalizedRequest.transportMode());

        return RouteEvaluateResponseDTO.builder()
                .originResolved(originResolved)
                .destinationResolved(destinationResolved)
                .transportMode(normalizedRequest.transportMode())
                .departureTime(normalizedRequest.departureTime())
                .routes(routes)
                .build();
    }

    private NormalizedRequest normalizeAndValidate(RouteEvaluateRequestDTO request) {
        if (request == null) {
            throw new InvalidRouteRequestException("El cuerpo de la solicitud es obligatorio.");
        }

        String origin = normalizeText(request.getOrigin());
        if (!StringUtils.hasText(origin)) {
            throw new InvalidRouteRequestException("El origen es obligatorio.");
        }

        String destination = normalizeText(request.getDestination());
        if (!StringUtils.hasText(destination)) {
            throw new InvalidRouteRequestException("El destino es obligatorio.");
        }

        if (origin.equalsIgnoreCase(destination)) {
            throw new InvalidRouteRequestException("El origen y destino no deben ser iguales.");
        }

        String transportMode = normalizeTransportMode(request.getTransportMode());
        return new NormalizedRequest(origin, destination, transportMode, request.getDepartureTime());
    }

    private void validateResolvedLocations(ResolvedPlaceDTO originResolved, ResolvedPlaceDTO destinationResolved) {
        if (originResolved.getLatitude() != null
                && originResolved.getLongitude() != null
                && destinationResolved.getLatitude() != null
                && destinationResolved.getLongitude() != null
                && Double.compare(originResolved.getLatitude(), destinationResolved.getLatitude()) == 0
                && Double.compare(originResolved.getLongitude(), destinationResolved.getLongitude()) == 0) {
            throw new InvalidRouteRequestException("El origen y destino resueltos no deben ser iguales.");
        }
    }

    private String normalizeTransportMode(String transportMode) {
        if (!StringUtils.hasText(transportMode)) {
            return DEFAULT_TRANSPORT_MODE;
        }

        String normalized = transportMode.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TRANSPORT_MODES.contains(normalized)) {
            throw new InvalidRouteRequestException(
                    "transportMode debe ser driving, walking o cycling.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : null;
    }

    private record NormalizedRequest(
            String origin,
            String destination,
            String transportMode,
            LocalDateTime departureTime) {
    }
}
