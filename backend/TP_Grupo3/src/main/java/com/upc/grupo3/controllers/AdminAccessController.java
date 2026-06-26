package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.auth.AuditLogResponseDTO;
import com.upc.grupo3.dtos.auth.SuspendUserRequestDTO;
import com.upc.grupo3.dtos.auth.UserReportHistoryResponseDTO;
import com.upc.grupo3.dtos.common.AdminAccessValidationRequestDTO;
import com.upc.grupo3.dtos.common.AdminAccessValidationResponseDTO;
import com.upc.grupo3.services.AdminAccessService;
import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAccessController {

    private final AdminAccessService adminAccessService;
    private final AuthService authService;

    @PostMapping("/validate-access")
    public ResponseEntity<AdminAccessValidationResponseDTO> validateAccess(
            @Valid @RequestBody AdminAccessValidationRequestDTO request,
            Authentication authentication) {
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        log.info("Validacion de acceso administrativo solicitada email={} resource={} action={}",
                authenticatedEmail, request.getResource(), request.getAction());

        AdminAccessValidationResponseDTO response =
                adminAccessService.validateAdministrativeAccess(request, authenticatedEmail);
        HttpStatus status = Boolean.TRUE.equals(response.getAuthorized()) ? HttpStatus.OK : HttpStatus.FORBIDDEN;

        log.info("Validacion de acceso administrativo resuelta email={} authorized={} role={}",
                authenticatedEmail, response.getAuthorized(), response.getRole());
        return ResponseEntity.status(status).body(response);
    }

    //HU22
    // Escenario 1: Visualizar historial de reportes falsos del usuario
    @GetMapping("/{id}/historial-falsos")
    public ResponseEntity<UserReportHistoryResponseDTO> getUserHistory(@PathVariable Integer id) {
        log.info("Petición GET para evaluar reincidencia de reportes falsos del usuario ID: {}", id);
        UserReportHistoryResponseDTO response = authService.getUserReportHistory(id);
        return ResponseEntity.ok(response);
    }

    // Escenario 2 y 3: Ejecutar la suspensión y guardar auditoría
    @PutMapping("/suspender")
    public ResponseEntity<String> suspendUser(
            @Valid @RequestBody SuspendUserRequestDTO request,
            Authentication authentication) {

        String adminEmail = authentication != null ? authentication.getName() : null;
        log.info("Petición PUT recibida para suspender al usuario ID: {}", request.getIdUsuario());

        String message = authService.suspendUserAccount(adminEmail, request);
        return ResponseEntity.ok(message);
    }

    // HU23 - Escenario 1, 2 y 3: Obtener perfil de reputación y reconocimientos del usuario activo
    @GetMapping("/mi-reputacion")
    public ResponseEntity<com.upc.grupo3.dtos.auth.UserReputationResponseDTO> getMyReputation(
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        log.info("Petición GET recibida para visualizar el panel de recompensas y reputación comunitaria.");

        com.upc.grupo3.dtos.auth.UserReputationResponseDTO response = authService.getUserReputation(email);
        return ResponseEntity.ok(response);
    }

    // HU24 - Escenario 1, 2 y 3: Obtener métricas consolidadas, agrupaciones y mapa de calor
    @GetMapping("/dashboard/metricas")
    public ResponseEntity<com.upc.grupo3.dtos.auth.DashboardMetricsResponseDTO> getMetrics(
            Authentication authentication) {

        String adminEmail = authentication != null ? authentication.getName() : null;
        log.info("Petición GET recibida en AdminUserController para procesar el panel de métricas urbanas.");

        com.upc.grupo3.dtos.auth.DashboardMetricsResponseDTO metrics = authService.getDashboardMetrics(adminEmail);
        return ResponseEntity.ok(metrics);
    }

    // HU25 - Escenario 1, 2 y 3: Emisión masiva de alertas de emergencia por el administrador
    @PostMapping("/alertas-globales/emitir")
    public ResponseEntity<com.upc.grupo3.dtos.privacy.AlertHistoryResponseDTO> emitGlobalAlert(
            @Valid @RequestBody com.upc.grupo3.dtos.privacy.GlobalAlertRequestDTO request,
            Authentication authentication) {

        String adminEmail = authentication != null ? authentication.getName() : null;
        log.info("Petición POST recibida para procesar transmisión masiva de alerta de emergencia.");

        com.upc.grupo3.dtos.privacy.AlertHistoryResponseDTO response = authService.broadcastGlobalAlert(adminEmail, request);
        return ResponseEntity.ok(response);
    }

    // HU26 - Escenario 2 y 3: Consulta y protección del historial de auditoría interna
    @GetMapping("/auditoria/historial")
    public ResponseEntity<List<com.upc.grupo3.dtos.auth.AuditLogResponseDTO>> getAuditHistory(
            Authentication authentication) {

        String adminEmail = authentication != null ? authentication.getName() : null;
        log.info("HU26 - Petición GET recibida para extraer registros de control interno.");

        List<AuditLogResponseDTO> history = authService.getAdminAuditLogs(adminEmail);
        return ResponseEntity.ok(history);
    }
}
