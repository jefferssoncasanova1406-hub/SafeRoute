package com.upc.av_2.services;

import com.upc.av_2.dtos.LoginRequestDTO;
import com.upc.av_2.dtos.LoginResponseDTO;
import com.upc.av_2.dtos.RegisterRequestDTO;
import com.upc.av_2.dtos.RegisterResponseDTO;
import com.upc.av_2.dtos.RegisteredUserDTO;
import com.upc.av_2.entidades.Perfil;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.EmailAlreadyRegisteredException;
import com.upc.av_2.exceptions.InvalidCredentialsException;
import com.upc.av_2.repositories.PerfilRepository;
import com.upc.av_2.repositories.RolRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
