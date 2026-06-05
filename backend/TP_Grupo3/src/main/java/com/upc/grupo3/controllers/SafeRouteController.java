package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.saferoute.SafeRouteRequestDTO;
import com.upc.grupo3.dtos.saferoute.SafeRouteResponseDTO;
import com.upc.grupo3.services.SafeRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
@Slf4j
public class SafeRouteController {

    private final SafeRouteService safeRouteService;

    //HU13
    @PostMapping("/calcular-segura")
    public ResponseEntity<SafeRouteResponseDTO> calculateSafeRoute(
            @Valid @RequestBody SafeRouteRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de calculo de ruta segura recibida email={}", authenticatedEmail);

        SafeRouteResponseDTO response = safeRouteService.calculateSafeRoute(authenticatedEmail, request);
        return ResponseEntity.ok(response);
    }
}
