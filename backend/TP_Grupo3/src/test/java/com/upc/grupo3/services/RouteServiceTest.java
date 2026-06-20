package com.upc.grupo3.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upc.grupo3.clients.MapboxClient;
import com.upc.grupo3.dtos.routeevaluation.GeoJsonLineStringDTO;
import com.upc.grupo3.dtos.routeevaluation.ResolvedPlaceDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateRequestDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateResponseDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteOptionDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteStepDTO;
import com.upc.grupo3.exceptions.InvalidRouteRequestException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private MapboxClient mapboxClient;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(mapboxClient);
    }

    @Test
    void evaluateRouteShouldDefaultTransportModeAndBuildNormalizedResponse() {
        RouteEvaluateRequestDTO request = RouteEvaluateRequestDTO.builder()
                .origin("Av. Javier Prado Este 4200, Santiago de Surco")
                .destination("UPC Monterrico")
                .departureTime(LocalDateTime.of(2026, 6, 17, 20, 30))
                .build();

        ResolvedPlaceDTO originResolved = ResolvedPlaceDTO.builder()
                .name("Av. Javier Prado Este 4200")
                .address("Av. Javier Prado Este 4200, Santiago de Surco, Lima, Peru")
                .latitude(-12.086)
                .longitude(-76.975)
                .build();
        ResolvedPlaceDTO destinationResolved = ResolvedPlaceDTO.builder()
                .name("UPC Monterrico")
                .address("Universidad Peruana de Ciencias Aplicadas, Santiago de Surco, Lima, Peru")
                .latitude(-12.104)
                .longitude(-76.963)
                .build();
        RouteOptionDTO route = RouteOptionDTO.builder()
                .routeId("route_1")
                .summary("Ruta 1")
                .durationMinutes(24.5)
                .distanceKm(8.4)
                .geometry(GeoJsonLineStringDTO.builder()
                        .type("LineString")
                        .coordinates(List.of(
                                List.of(-76.975, -12.086),
                                List.of(-76.963, -12.104)))
                        .build())
                .steps(List.of(RouteStepDTO.builder()
                        .order(1)
                        .instruction("Avanza por Av. Javier Prado Este")
                        .streetName("Av. Javier Prado Este")
                        .distanceMeters(900d)
                        .durationSeconds(180d)
                        .maneuverType("depart")
                        .build()))
                .build();

        when(mapboxClient.geocode(request.getOrigin(), "origen")).thenReturn(originResolved);
        when(mapboxClient.geocode(request.getDestination(), "destino")).thenReturn(destinationResolved);
        when(mapboxClient.getDirections(originResolved, destinationResolved, "driving"))
                .thenReturn(List.of(route));

        RouteEvaluateResponseDTO response = routeService.evaluateRoute(request);

        assertNotNull(response);
        assertEquals("driving", response.getTransportMode());
        assertEquals(request.getDepartureTime(), response.getDepartureTime());
        assertEquals(originResolved.getAddress(), response.getOriginResolved().getAddress());
        assertEquals(destinationResolved.getAddress(), response.getDestinationResolved().getAddress());
        assertEquals(1, response.getRoutes().size());
        assertEquals("route_1", response.getRoutes().get(0).getRouteId());

        verify(mapboxClient).getDirections(originResolved, destinationResolved, "driving");
    }

    @Test
    void evaluateRouteShouldRejectInvalidTransportMode() {
        RouteEvaluateRequestDTO request = RouteEvaluateRequestDTO.builder()
                .origin("Origen")
                .destination("Destino")
                .transportMode("bus")
                .build();

        InvalidRouteRequestException exception = assertThrows(
                InvalidRouteRequestException.class,
                () -> routeService.evaluateRoute(request));

        assertEquals("transportMode debe ser driving, walking o cycling.", exception.getMessage());
    }

    @Test
    void evaluateRouteShouldRejectEqualOriginAndDestinationTexts() {
        RouteEvaluateRequestDTO request = RouteEvaluateRequestDTO.builder()
                .origin("UPC Monterrico")
                .destination("  upc monterrico ")
                .build();

        InvalidRouteRequestException exception = assertThrows(
                InvalidRouteRequestException.class,
                () -> routeService.evaluateRoute(request));

        assertEquals("El origen y destino no deben ser iguales.", exception.getMessage());
    }
}
