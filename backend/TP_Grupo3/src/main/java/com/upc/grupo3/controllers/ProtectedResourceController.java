package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.auth.AuthenticatedUserDTO;
import com.upc.grupo3.dtos.auth.PasswordChangeRequestDTO;
import com.upc.grupo3.dtos.common.ProtectedResourceResponseDTO;
import java.util.List;

import com.upc.grupo3.services.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import com.upc.grupo3.services.AuthService;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/secure")
@RequiredArgsConstructor
@Slf4j
public class ProtectedResourceController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ProtectedResourceResponseDTO> getAuthenticatedUser(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        log.info("Acceso a recurso protegido subject={} authorities={}", authentication.getName(), authorities);

        ProtectedResourceResponseDTO response = ProtectedResourceResponseDTO.builder()
                .message("Acceso autorizado")
                .user(AuthenticatedUserDTO.builder()
                        .email(authentication.getName())
                        .authorities(authorities)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    //HU5
    // Añadir al ProtectedResourceController.java
    // Importa el DTO y el AuthService arriba
    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody PasswordChangeRequestDTO request,
            Authentication authentication) {
        // Obtenemos el email directamente del token de sesión
        String email = authentication.getName();

        authService.updatePasswordFromProfile(email, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }
}
