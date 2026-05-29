package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.riskzone.RiskZoneMapRequestDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapResponseDTO;
import com.upc.grupo3.services.RiskZoneMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mapa/zonas-riesgo")
@RequiredArgsConstructor
@Slf4j
public class RiskZoneMapController {

    private final RiskZoneMapService riskZoneMapService;

    @GetMapping("/activas")
    public ResponseEntity<RiskZoneMapResponseDTO> getActiveRiskZones(
            @Valid @ModelAttribute RiskZoneMapRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Consulta de mapa recibida email={} ciudad={} distrito={}",
                authenticatedEmail, request.getCiudad(), request.getDistrito());

        RiskZoneMapResponseDTO response =
                riskZoneMapService.getActiveRiskZonesForMap(authenticatedEmail, request);
        return ResponseEntity.ok(response);
    }
}
