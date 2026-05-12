package com.upc.av_2.controllers;

import com.upc.av_2.dtos.AuthenticatedUserDTO;
import com.upc.av_2.dtos.ProtectedResourceResponseDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secure")
@Slf4j
public class ProtectedResourceController {

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
}
