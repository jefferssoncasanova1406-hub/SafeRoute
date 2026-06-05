package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.PrivacyPreferencesRequestDTO;
import com.upc.grupo3.dtos.privacy.PrivacyPreferencesResponseDTO;
import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
@Slf4j
public class PrivacyPreferencesController {

    private final AuthService authService;

    // Escenario 1: Visualizar configuraciones actuales de avisos y notificaciones
    @GetMapping("/preferences")
    public ResponseEntity<PrivacyPreferencesResponseDTO> getPreferences(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU17 - Solicitud de visualización de alertas recibida para el usuario autenticado.");

        PrivacyPreferencesResponseDTO response = authService.getPrivacyPreferences(email);
        return ResponseEntity.ok(response);
    }

    // Escenario 2 y 3: Modificar y salvar niveles mínimos de riesgo y tipos de incidentes
    @PutMapping("/preferences")
    public ResponseEntity<PrivacyPreferencesResponseDTO> updatePreferences(
            @Valid @RequestBody PrivacyPreferencesRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU17 - Guardando cambios de alertas en base de datos para el usuario autenticado.");

        PrivacyPreferencesResponseDTO response = authService.updatePrivacyPreferences(email, request);
        return ResponseEntity.ok(response);
    }
}
