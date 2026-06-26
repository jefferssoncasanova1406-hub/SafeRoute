package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.LanguageChangeRequestDTO;
import com.upc.grupo3.dtos.privacy.LanguageConfigResponseDTO;
import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/config/idioma")
@RequiredArgsConstructor
@Slf4j
public class LanguageConfigController {

    private final AuthService authService;

    // Escenario 1: Visualización de idiomas soportados oficialmente por SafeRoute
    @GetMapping("/disponibles")
    public ResponseEntity<List<String>> getAvailableLanguages() {
        log.info("HU29 - Petición GET recibida para listar idiomas disponibles.");
        List<String> idiomas = Arrays.asList("es (Español)", "en (English)");
        return ResponseEntity.ok(idiomas);
    }

    // Escenario 3: Cargar configuración actual o la predeterminada por defecto
    @GetMapping("/actual")
    public ResponseEntity<LanguageConfigResponseDTO> getCurrentLanguage(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU29 - Recuperando idioma activo de la sesión.");
        LanguageConfigResponseDTO response = authService.getCurrentLanguagePreference(email);
        return ResponseEntity.ok(response);
    }

    // Escenario 2: Cambio exitoso de idioma con sesión activa
    @PutMapping("/cambiar")
    public ResponseEntity<LanguageConfigResponseDTO> changeLanguage(
            @Valid @RequestBody LanguageChangeRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU29 - Petición PUT recibida para mutar el idioma de la interfaz.");
        LanguageConfigResponseDTO response = authService.updateLanguagePreference(email, request);
        return ResponseEntity.ok(response);
    }
}
