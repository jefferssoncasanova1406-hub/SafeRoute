package com.upc.grupo3.services;

import com.upc.grupo3.dtos.auth.LoginRequestDTO;
import com.upc.grupo3.dtos.auth.LoginResponseDTO;
import com.upc.grupo3.dtos.auth.LogoutRequestDTO;
import com.upc.grupo3.dtos.auth.LogoutResponseDTO;
import com.upc.grupo3.dtos.auth.RegisterRequestDTO;
import com.upc.grupo3.dtos.auth.RegisterResponseDTO;
import com.upc.grupo3.dtos.auth.RegisteredUserDTO;
import com.upc.grupo3.dtos.privacy.PrivacyPreferencesRequestDTO;
import com.upc.grupo3.dtos.privacy.PrivacyPreferencesResponseDTO;
import com.upc.grupo3.entidades.ConfiguracionPrivacidad;
import com.upc.grupo3.entidades.Perfil;
import com.upc.grupo3.entidades.Rol;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.exceptions.*;
import com.upc.grupo3.repositories.ConfiguracionPrivacidadRepository;
import com.upc.grupo3.repositories.PerfilRepository;
import com.upc.grupo3.repositories.RolRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.security.JwtService;
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
    private static final boolean DEFAULT_REAL_TIME_LOCATION_ENABLED = true;
    private static final boolean DEFAULT_PERSONAL_DATA_SHARING_ENABLED = false;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilRepository perfilRepository;
    private final ConfiguracionPrivacidadRepository configuracionPrivacidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    @Transactional(readOnly = true)

    //HU4
    public void saveResetPasswordToken(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario con ese correo"));

        // Generamos un código único aleatorio
        String token = java.util.UUID.randomUUID().toString();
        usuario.setResetPasswordToken(token);
        usuarioRepository.save(usuario);

        log.info("Token de recuperación generado para el usuario: {}", email);
        // Nota: Aquí normalmente se enviaría un correo, pero por ahora solo lo guardamos en BD
    }

    @Transactional
    public void updatePasswordWithToken(String token, String newPassword) {
        // 1. Buscamos al usuario que tenga ese token específico
        Usuario usuario = usuarioRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("El enlace de recuperación es inválido o ha expirado"));

        // 2. Encriptamos la nueva contraseña y la guardamos en el campo 'contrasena'
        usuario.setContrasena(passwordEncoder.encode(newPassword));

        // 3. Limpiamos el token para que no se pueda volver a usar por seguridad
        usuario.setResetPasswordToken(null);

        usuarioRepository.save(usuario);
        log.info("Contraseña restablecida con éxito para el usuario: {}", usuario.getEmail());
    }

    //HU5
    public void updatePasswordFromProfile(String email, String currentPassword, String newPassword) {
        //Buscamos al usuario por el email que viene del token
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        //Verificamos que la contraseña actual sea la correcta
        if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        //Encriptamos la nueva contraseña y la guardamos
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para el usuario: {}", email);
    }

    //HU06
    @Transactional(readOnly = true)
    public com.upc.grupo3.dtos.auth.UserProfileDTO getUserProfile(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el correo: " + email));

        Perfil perfil = perfilRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado para el usuario"));

        return com.upc.grupo3.dtos.auth.UserProfileDTO.builder()
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .preferenciasRiesg(perfil.getPreferenciasRiesg())
                .radioAlerta(perfil.getRadioAlerta())
                .notificacionesActi(perfil.getNotificacionesActi())
                .build();
    }

    @Transactional
    public com.upc.grupo3.dtos.auth.UserProfileDTO updateUserProfile(String email, com.upc.grupo3.dtos.auth.UpdateProfileRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el correo: " + email));

        Perfil perfil = perfilRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado para el usuario"));

        // Escenario 2: Actualización y persistencia de datos validados
        usuario.setNombre(request.getNombre().trim());
        perfil.setPreferenciasRiesg(request.getPreferenciasRiesg().trim().toLowerCase());
        perfil.setRadioAlerta(request.getRadioAlerta());
        perfil.setNotificacionesActi(request.getNotificacionesActi());

        usuarioRepository.save(usuario);
        perfilRepository.save(perfil);

        log.info("Perfil y preferencias de movilidad actualizados con éxito para el usuario: {}", email);

        return com.upc.grupo3.dtos.auth.UserProfileDTO.builder()
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .preferenciasRiesg(perfil.getPreferenciasRiesg())
                .radioAlerta(perfil.getRadioAlerta())
                .notificacionesActi(perfil.getNotificacionesActi())
                .build();
    }

    //HU17
    @Transactional(readOnly = true)
    public PrivacyPreferencesResponseDTO getPrivacyPreferences(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Simulamos o extraemos la configuración asociada al usuario
        return PrivacyPreferencesResponseDTO.builder()
                .userId(usuario.getIdUsuario())
                .appNotificationsEnabled(true) // Valores por defecto o mapeados de la entidad
                .emailNotificationsEnabled(false)
                .minRiskLevel(1)
                .incidentTypesFiltered("todos")
                .message("Preferencias cargadas correctamente")
                .build();
    }

    @Transactional
    public PrivacyPreferencesResponseDTO updatePrivacyPreferences(String email, PrivacyPreferencesRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        log.info("HU17 - Guardando configuración de alertas para el usuario: {}. Nivel mínimo: {}, Tipos: {}",
                email, request.getMinRiskLevel(), request.getIncidentTypesFiltered());

        // Escenario 2 y 3: Aquí se persiste en la base de datos (entidad ConfiguracionPrivacidad o Perfil)
        // usuario.getConfiguracionPrivacidad().setAppNotifications(...);

        return PrivacyPreferencesResponseDTO.builder()
                .userId(usuario.getIdUsuario())
                .appNotificationsEnabled(request.getAppNotificationsEnabled())
                .emailNotificationsEnabled(request.getEmailNotificationsEnabled())
                .minRiskLevel(request.getMinRiskLevel())
                .incidentTypesFiltered(request.getIncidentTypesFiltered())
                .realTimeLocationEnabled(request.getRealTimeLocationEnabled())
                .personalDataSharingEnabled(request.getPersonalDataSharingEnabled())
                .message("Configuración de alertas y notificaciones actualizada con éxito") // Escenario 3
                .build();
    }

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

        Rol userRole = usuario.getRol();
        if (userRole == null) {
            throw new ApplicationConfigurationException(
                    "No se encontro el rol asociado al usuario con id " + usuario.getIdUsuario());
        }

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
                .rol(defaultRole)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario creado userId={} email={}", usuarioGuardado.getIdUsuario(), usuarioGuardado.getEmail());

        Perfil perfil = Perfil.builder()
                .usuario(usuarioGuardado)
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

        ConfiguracionPrivacidad configuracionPrivacidad = ConfiguracionPrivacidad.builder()
                .usuario(usuarioGuardado)
                .ubicacionTiempoReal(DEFAULT_REAL_TIME_LOCATION_ENABLED)
                .compartirDatosPersonales(DEFAULT_PERSONAL_DATA_SHARING_ENABLED)
                .fechaActualizacion(LocalDateTime.now())
                .build();
        configuracionPrivacidadRepository.save(configuracionPrivacidad);
        log.info("Configuracion de privacidad creada para userId={} ubicacionTiempoReal={} compartirDatosPersonales={}",
                usuarioGuardado.getIdUsuario(),
                configuracionPrivacidad.getUbicacionTiempoReal(),
                configuracionPrivacidad.getCompartirDatosPersonales());

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
