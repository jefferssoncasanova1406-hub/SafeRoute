package com.upc.grupo3.services;

import com.upc.grupo3.entidades.TokenRevocado;
import com.upc.grupo3.repositories.TokenRevocadoRepository;
import com.upc.grupo3.security.JwtService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationService {

    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final JwtService jwtService;

    @Transactional
    public void revokeToken(String token, String username, LocalDateTime expirationAt) {
        String tokenHash = jwtService.hashToken(token);
        if (tokenRevocadoRepository.existsByTokenHash(tokenHash)) {
            log.debug("Token ya revocado para email={}", username);
            return;
        }

        TokenRevocado tokenRevocado = TokenRevocado.builder()
                .tokenHash(tokenHash)
                .usuarioEmail(username)
                .fechaRevocacion(LocalDateTime.now())
                .fechaExpiracion(expirationAt)
                .build();
        tokenRevocadoRepository.save(tokenRevocado);
        log.info("Token revocado para email={} expiraEn={}", username, expirationAt);
    }

    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String token) {
        boolean revoked = tokenRevocadoRepository.existsByTokenHash(jwtService.hashToken(token));
        if (revoked) {
            log.debug("Se detecto un token previamente revocado");
        }
        return revoked;
    }
}
