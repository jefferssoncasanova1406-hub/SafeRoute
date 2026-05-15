package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.common.AdminAccessValidationRequestDTO;
import com.upc.grupo3.dtos.common.AdminAccessValidationResponseDTO;
import com.upc.grupo3.services.AdminAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAccessController {

    private final AdminAccessService adminAccessService;

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
}
