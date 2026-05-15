package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.PrivacyPreferencesRequestDTO;
import com.upc.grupo3.dtos.privacy.PrivacyPreferencesResponseDTO;
import com.upc.grupo3.services.PrivacyPreferencesService;
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
@RequestMapping("/api/privacy/preferences")
@RequiredArgsConstructor
@Slf4j
public class PrivacyPreferencesController {

    private final PrivacyPreferencesService privacyPreferencesService;

    @GetMapping
    public ResponseEntity<PrivacyPreferencesResponseDTO> getPreferences(Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Consulta de preferencias de privacidad solicitada email={}", authenticatedEmail);

        PrivacyPreferencesResponseDTO response = privacyPreferencesService.getPreferences(authenticatedEmail);
        log.info("Consulta de preferencias de privacidad completada userId={}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PrivacyPreferencesResponseDTO> updatePreferences(
            @Valid @RequestBody PrivacyPreferencesRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Actualizacion de preferencias de privacidad solicitada email={}", authenticatedEmail);

        PrivacyPreferencesResponseDTO response =
                privacyPreferencesService.updatePreferences(authenticatedEmail, request);
        log.info("Actualizacion de preferencias de privacidad completada userId={}", response.getUserId());
        return ResponseEntity.ok(response);
    }
}
