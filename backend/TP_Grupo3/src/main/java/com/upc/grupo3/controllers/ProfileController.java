package com.upc.grupo3.controllers;

import com.upc.grupo3.dtos.auth.UserProfileDTO;
import com.upc.grupo3.dtos.auth.UpdateProfileRequestDTO;
import com.upc.grupo3.services.AuthService;
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
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final AuthService authService;

    // Escenario 1: Visualización de datos actuales
    @GetMapping
    public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de visualización de perfil para el usuario autenticado: {}", email);

        UserProfileDTO profile = authService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    // Escenario 2: Actualización exitosa del perfil
    @PutMapping
    public ResponseEntity<UserProfileDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        log.info("Solicitud de actualización de perfil para el usuario autenticado: {}", email);

        UserProfileDTO updatedProfile = authService.updateUserProfile(email, request);
        return ResponseEntity.ok(updatedProfile);
    }
}
