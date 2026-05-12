package com.upc.av_2.services;

import com.upc.av_2.dtos.LoginRequestDTO;
import com.upc.av_2.dtos.LoginResponseDTO;
import com.upc.av_2.dtos.LogoutRequestDTO;
import com.upc.av_2.dtos.LogoutResponseDTO;
import com.upc.av_2.dtos.RegisterRequestDTO;
import com.upc.av_2.dtos.RegisterResponseDTO;
import com.upc.av_2.dtos.RegisteredUserDTO;
import com.upc.av_2.entidades.Perfil;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.EmailAlreadyRegisteredException;
import com.upc.av_2.exceptions.InvalidAuthorizationHeaderException;
import com.upc.av_2.exceptions.InvalidCredentialsException;
import com.upc.av_2.exceptions.TokenOwnershipException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.PerfilRepository;
import com.upc.av_2.repositories.RolRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String DEFAULT_ROLE_NAME = "usuario";
    private static final String DEFAULT_RISK_PREFERENCE = "medio";
    private static final BigDecimal DEFAULT_ALERT_RADIUS = new BigDecimal("1.0000000");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.debug("Iniciando login para email={}", normalizedEmail);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login rechazado por credenciales invalidas email={}", normalizedEmail);
                    return new InvalidCredentialsException("Credenciales invalidas");
                });

        if (!passwordEncoder.matches(request.getPassword(), usuario.getContrasena())) {
            log.warn("Login rechazado por credenciales invalidas email={}", normalizedEmail);
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Login rechazado por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), normalizedEmail);
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        Rol userRole = rolRepository.findById(usuario.getRolIdRol())
                .orElseThrow(() -> new ApplicationConfigurationException(
                        "No se encontro el rol asociado al usuario con id " + usuario.getIdUsuario()));

        String token = jwtService.generateToken(usuario, userRole.getNombre());
        RegisteredUserDTO authenticatedUser = RegisteredUserDTO.builder()
                .id(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(userRole.getNombre())
                .build();

        log.info("Login exitoso userId={} email={} rol={}",
                usuario.getIdUsuario(), usuario.getEmail(), userRole.getNombre());

        return LoginResponseDTO.builder()
                .message("Inicio de sesion exitoso")
                .token(token)
                .tokenType("Bearer")
                .user(authenticatedUser)
                .build();
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.debug("Iniciando registro de usuario para email={}", normalizedEmail);
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Intento de registro con correo duplicado email={}", normalizedEmail);
            throw new EmailAlreadyRegisteredException("El correo ya esta registrado");
        }

        Rol defaultRole = rolRepository.findByNombreIgnoreCase(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new ApplicationConfigurationException(
                        "No se encontro el rol por defecto '" + DEFAULT_ROLE_NAME + "'"));
        log.debug("Rol por defecto resuelto para registro email={} rolId={} rol={}",
                normalizedEmail, defaultRole.getIdRol(), defaultRole.getNombre());

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .email(normalizedEmail)
                .contrasena(passwordEncoder.encode(request.getPassword()))
                .fechaRegistro(LocalDate.now())
                .estado(Boolean.TRUE)
                .rolIdRol(defaultRole.getIdRol())
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario creado userId={} email={}", usuarioGuardado.getIdUsuario(), usuarioGuardado.getEmail());

        Perfil perfil = Perfil.builder()
                .idUsuario(usuarioGuardado.getIdUsuario())
                .preferenciasRiesg(DEFAULT_RISK_PREFERENCE)
                .radioAlerta(DEFAULT_ALERT_RADIUS)
                .notificacionesActi(Boolean.TRUE)
                .build();
        perfilRepository.save(perfil);
        log.info("Perfil creado para userId={} preferenciasRiesgo={} radioAlerta={} notificacionesActivas={}",
                usuarioGuardado.getIdUsuario(),
                perfil.getPreferenciasRiesg(),
                perfil.getRadioAlerta(),
                perfil.getNotificacionesActi());

        RegisteredUserDTO registeredUser = RegisteredUserDTO.builder()
                .id(usuarioGuardado.getIdUsuario())
                .nombre(usuarioGuardado.getNombre())
                .email(usuarioGuardado.getEmail())
                .rol(defaultRole.getNombre())
                .build();

        return RegisterResponseDTO.builder()
                .message("Usuario registrado correctamente")
                .user(registeredUser)
                .build();
    }

    @Transactional
    public LogoutResponseDTO logout(String authorizationHeader, LogoutRequestDTO request, String authenticatedEmail) {
        validateAuthenticatedUser(authenticatedEmail);
        String token = extractBearerToken(authorizationHeader);
        validateTokenState(token);

        String username = extractUsername(token);
        validateTokenOwnership(authenticatedEmail, username);

        Date expiration = extractExpiration(token);
        LocalDateTime expirationAt = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());

        tokenRevocationService.revokeToken(token, username, expirationAt);
        log.info("Logout exitoso email={} expiraEn={}", username, expirationAt);

        return LogoutResponseDTO.builder()
                .success(Boolean.TRUE)
                .message("Sesion cerrada correctamente")
                .build();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Logout rechazado por encabezado Authorization invalido");
            throw new InvalidAuthorizationHeaderException(
                    "El encabezado " + HttpHeaders.AUTHORIZATION + " debe usar el esquema Bearer");
        }

        String token = authorizationHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            log.warn("Logout rechazado por token vacio en Authorization header");
            throw new InvalidAuthorizationHeaderException("El token JWT no puede estar vacio");
        }
        return token;
    }

    private void validateAuthenticatedUser(String authenticatedEmail) {
        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Logout rechazado porque no existe usuario autenticado en el contexto de seguridad");
            throw new UnauthenticatedUserException("No existe una sesion autenticada para cerrar");
        }
    }

    private void validateTokenState(String token) {
        if (!jwtService.isTokenValid(token)) {
            log.warn("Logout rechazado porque el token JWT es invalido o expirado");
            throw new InvalidAuthorizationHeaderException("El token JWT es invalido o ha expirado");
        }

        if (tokenRevocationService.isTokenRevoked(token)) {
            log.warn("Logout rechazado porque el token ya fue revocado");
            throw new InvalidAuthorizationHeaderException("La sesion ya fue cerrada");
        }
    }

    private String extractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("Logout rechazado porque no se pudo extraer el usuario del token: {}", exception.getMessage());
            throw new InvalidAuthorizationHeaderException("El token JWT es invalido o ha expirado");
        }
    }

    private Date extractExpiration(String token) {
        try {
            return jwtService.extractExpiration(token);
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("Logout rechazado porque no se pudo extraer la expiracion del token: {}", exception.getMessage());
            throw new InvalidAuthorizationHeaderException("El token JWT es invalido o ha expirado");
        }
    }

    private void validateTokenOwnership(String authenticatedEmail, String tokenUsername) {
        if (!authenticatedEmail.equalsIgnoreCase(tokenUsername)) {
            log.warn("Logout rechazado por incongruencia entre usuario autenticado={} y token={}",
                    authenticatedEmail, tokenUsername);
            throw new TokenOwnershipException("El token no pertenece al usuario autenticado");
        }
    }
}
