package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.privacy.AlertHistoryRequestDTO;
import com.upc.grupo3.dtos.privacy.AlertHistoryResponseDTO;
import com.upc.grupo3.dtos.privacy.CommunityVoteRequestDTO;
import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//HU18 y HU19:
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@Slf4j
public class AlertsHistoryController {

    private final AuthService authService;

    // Escenario 1 y 3: Resumen reciente y listado filtrado de alertas históricas
    @GetMapping("/historial")
    public ResponseEntity<List<AlertHistoryResponseDTO>> getHistory(
            @RequestParam(required = false) String tipoIncidente,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de historial de alertas procesada para el usuario.");

        List<AlertHistoryResponseDTO> history = authService.getAlertsHistory(email, tipoIncidente, estado, fechaInicio, fechaFin);
        return ResponseEntity.ok(history);
    }

    // Escenario 2: Consultar y abrir el detalle de una alerta seleccionada
    @GetMapping("/detalle/{id}")
    public ResponseEntity<AlertHistoryResponseDTO> getDetail(
            @PathVariable Integer id,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de detalle para alerta con ID: {}", id);

        AlertHistoryResponseDTO detail = authService.getAlertDetail(email, id);
        return ResponseEntity.ok(detail);
    }

    // HU19 - Escenario 1, 2 y 3: Registro ciudadano de incidentes (Agregado aquí)
    @PostMapping("/reportar")
    public ResponseEntity<AlertHistoryResponseDTO> createReport(
            @Valid @RequestBody AlertHistoryRequestDTO request,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        log.info("HU19 - Petición POST recibida para registrar un reporte ciudadano en AlertsHistoryController.");

        AlertHistoryResponseDTO response = authService.registerIncidentReport(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // HU20 - Escenario 1, 2 y 3: Confirmación, rechazo y bloqueo de voto duplicado
    @PostMapping("/verificar")
    public ResponseEntity<AlertHistoryResponseDTO> verifyIncident(
            @Valid @RequestBody CommunityVoteRequestDTO request,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        log.info("HU20 - Petición POST recibida para verificación comunitaria del incidente {}", request.getIdIncidente());

        AlertHistoryResponseDTO response = authService.verifyCommunityIncident(email, request);
        return ResponseEntity.ok(response);
    }
}
