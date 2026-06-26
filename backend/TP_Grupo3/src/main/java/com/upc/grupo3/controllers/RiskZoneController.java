package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.riskzone.RiskZoneDetailDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneListResponseDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneOperationResponseDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneRequestDTO;
import com.upc.grupo3.services.RiskZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/risk-zones")
@RequiredArgsConstructor
@Slf4j
public class RiskZoneController {

    private final RiskZoneService riskZoneService;

    @PostMapping
    public ResponseEntity<RiskZoneOperationResponseDTO> createRiskZone(
            @Valid @RequestBody RiskZoneRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de creacion de zona de riesgo recibida email={} tipo={} nivelRiesgo={}",
                authenticatedEmail, request.getTipo(), request.getNivelRiesgo());

        RiskZoneOperationResponseDTO response = riskZoneService.createRiskZone(authenticatedEmail, request);
        log.info("Creacion de zona de riesgo completada email={} zoneId={}",
                authenticatedEmail, response.getZona().getIdZona());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RiskZoneOperationResponseDTO> updateRiskZone(
            @PathVariable("id") Integer idZona,
            @Valid @RequestBody RiskZoneRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de actualizacion de zona de riesgo recibida email={} zoneId={}",
                authenticatedEmail, idZona);

        RiskZoneOperationResponseDTO response = riskZoneService.updateRiskZone(authenticatedEmail, idZona, request);
        log.info("Actualizacion de zona de riesgo completada email={} zoneId={}",
                authenticatedEmail, response.getZona().getIdZona());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RiskZoneOperationResponseDTO> deactivateRiskZone(
            @PathVariable("id") Integer idZona,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de desactivacion de zona de riesgo recibida email={} zoneId={}",
                authenticatedEmail, idZona);

        RiskZoneOperationResponseDTO response = riskZoneService.deactivateRiskZone(authenticatedEmail, idZona);
        log.info("Desactivacion de zona de riesgo completada email={} zoneId={}",
                authenticatedEmail, response.getZona().getIdZona());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RiskZoneOperationResponseDTO> deleteRiskZone(
            @PathVariable("id") Integer idZona,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de eliminacion logica de zona de riesgo recibida email={} zoneId={}",
                authenticatedEmail, idZona);

        RiskZoneOperationResponseDTO response = riskZoneService.deactivateRiskZone(authenticatedEmail, idZona);
        log.info("Eliminacion logica de zona de riesgo completada email={} zoneId={}",
                authenticatedEmail, response.getZona().getIdZona());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RiskZoneListResponseDTO> getRiskZones(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer nivelRiesgo,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Consulta de zonas de riesgo solicitada email={} estado={} nivelRiesgo={}",
                authenticatedEmail, estado, nivelRiesgo);

        RiskZoneListResponseDTO response =
                riskZoneService.getRiskZones(authenticatedEmail, estado, nivelRiesgo);
        log.info("Consulta de zonas de riesgo completada email={} total={}",
                authenticatedEmail, response.getZonas().size());
        return ResponseEntity.ok(response);
    }

    //HU14
    @GetMapping("/active-alerts")
    public ResponseEntity<RiskZoneListResponseDTO> getActiveAlerts() {
        log.info("Consulta simplificada de alertas activas para el mapa solicitada");

        // Llamamos al nuevo metodo que añadimos en el RiskZoneService
        RiskZoneListResponseDTO response = riskZoneService.getActiveAlertsForMap();

        log.info("Consulta de alertas activas completada total={}", response.getZonas().size());
        return ResponseEntity.ok(response);
    }

    //HU15
    @GetMapping("/{id}")
    public ResponseEntity<RiskZoneDetailDTO> getRiskZoneDetail(@PathVariable("id") Integer idZona) {
        log.info("Solicitud de detalle de alerta recibida zoneId={}", idZona);

        RiskZoneDetailDTO detail = riskZoneService.getRiskZoneDetail(idZona);

        return ResponseEntity.ok(detail);
    }

    //HU16
    @GetMapping("/check-proximity")
    public ResponseEntity<Map<String, Object>> checkProximity(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : "anónimo";
        return ResponseEntity.ok(riskZoneService.checkNearbyRiskZones(email, lat, lon));
    }
}
