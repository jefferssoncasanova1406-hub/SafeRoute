package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.PublicTrackingResponseDTO;
import com.upc.grupo3.dtos.privacy.ShareLinkResponseDTO;
import com.upc.grupo3.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
public class RouteTrackingController {

    private final AuthService authService;
    //HU27
    // Escenario 1: Generar el enlace dinámico seguro
    @PostMapping("/compartir")
    public ResponseEntity<ShareLinkResponseDTO> startSharing(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("Solicitud para iniciar compartición de ubicación en tiempo real.");
        ShareLinkResponseDTO response = authService.generateTrackingLink(email);
        return ResponseEntity.ok(response);
    }

    // Escenario 2: Vista pública consumida por el contacto (Configurar permitAll() en backend para este sub-path)
    @GetMapping("/public/consultar/{token}")
    public ResponseEntity<PublicTrackingResponseDTO> viewPublicTracking(@PathVariable String token) {
        log.info("Petición anónima entrante para visualizar la ubicación del token: {}", token);
        PublicTrackingResponseDTO trackingData = authService.getPublicTrackingData(token);
        return ResponseEntity.ok(trackingData);
    }

    // Escenario 3: Forzar expiración o corte manual por parte del dueño de la ruta
    @PutMapping("/detener/{token}")
    public ResponseEntity<String> stopSharing(
            @PathVariable String token,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("Cancelando la transmisión pública del trayecto.");
        String message = authService.revokeTrackingLink(email, token);
        return ResponseEntity.ok(message);
    }
}
