package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.CityDetectionRequestDTO;
import com.upc.grupo3.dtos.privacy.CityOperationResponseDTO;
import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config/ciudad")
@RequiredArgsConstructor
@Slf4j
public class CityOperationController {

    private final AuthService authService;

    // Escenario 1 y 3: Selección o cambio manual de la ciudad
    @PutMapping("/seleccionar")
    public ResponseEntity<CityOperationResponseDTO> changeCity(
            @RequestParam String ciudad,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU28 - Petición PUT para cambiar manualmente la ciudad operativa.");
        CityOperationResponseDTO response = authService.selectCityManually(email, ciudad);
        return ResponseEntity.ok(response);
    }

    // Escenario 2 y 3: Intentar detectar automáticamente con el GPS del móvil
    @PostMapping("/detectar")
    public ResponseEntity<CityOperationResponseDTO> detectCity(
            @Valid @RequestBody CityDetectionRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU28 - Petición POST recibida para detección automática de ciudad.");
        CityOperationResponseDTO response = authService.detectCityAutomatically(email, request);
        return ResponseEntity.ok(response);
    }
}
