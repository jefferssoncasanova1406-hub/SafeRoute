package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateRequestDTO;
import com.upc.grupo3.dtos.routeevaluation.RouteEvaluateResponseDTO;
import com.upc.grupo3.services.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/evaluate")
    public ResponseEntity<RouteEvaluateResponseDTO> evaluateRoute(
            @RequestBody(required = false) RouteEvaluateRequestDTO request) {
        log.info("Solicitud de evaluacion de rutas recibida");
        return ResponseEntity.ok(routeService.evaluateRoute(request));
    }
}
