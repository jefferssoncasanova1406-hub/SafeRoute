package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.MapThemeRequestDTO;
import com.upc.grupo3.dtos.privacy.MapThemeResponseDTO;
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
@RequestMapping("/api/config/mapa-tema")
@RequiredArgsConstructor
@Slf4j
public class MapThemeConfigController {

    private final AuthService authService;

    // Escenario 1: Visualización de los temas de mapas disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<String>> getAvailableThemes() {
        log.info("HU30 - Petición GET recibida para listar temas cartográficos soportados.");
        List<String> temas = Arrays.asList("LIGHT (Claro Estándar)", "DARK (Oscuro Estético)", "SATELLITE (Satelital Híbrido)");
        return ResponseEntity.ok(temas);
    }

    // Escenario 3: Recuperar y cargar de forma automática la preferencia guardada al iniciar
    @GetMapping("/actual")
    public ResponseEntity<MapThemeResponseDTO> getCurrentTheme(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU30 - Consultando la configuración visual activa del mapa.");
        MapThemeResponseDTO response = authService.getMapThemePreference(email);
        return ResponseEntity.ok(response);
    }

    // Escenario 2: Cambio inmediato de apariencia
    @PutMapping("/cambiar")
    public ResponseEntity<MapThemeResponseDTO> changeTheme(
            @Valid @RequestBody MapThemeRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("HU30 - Petición PUT para actualizar el vector de diseño del mapa.");
        MapThemeResponseDTO response = authService.updateMapThemePreference(email, request);
        return ResponseEntity.ok(response);
    }
}
