package com.upc.av_2.controllers;

import com.upc.av_2.dtos.LoginRequestDTO;
import com.upc.av_2.dtos.LoginResponseDTO;
import com.upc.av_2.dtos.RegisterRequestDTO;
import com.upc.av_2.dtos.RegisterResponseDTO;
import com.upc.av_2.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Solicitud de login recibida para email={}", request.getEmail());
        LoginResponseDTO response = authService.login(request);
        log.info("Login completado para userId={} email={}", response.getUser().getId(), response.getUser().getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Solicitud de registro recibida para email={}", request.getEmail());
        RegisterResponseDTO response = authService.register(request);
        log.info("Registro completado para userId={} email={}", response.getUser().getId(), response.getUser().getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
