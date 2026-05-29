package com.upc.grupo3.services;

import com.upc.grupo3.dtos.privacy.PrivacyPreferencesRequestDTO;
import com.upc.grupo3.dtos.privacy.PrivacyPreferencesResponseDTO;
import com.upc.grupo3.entidades.ConfiguracionPrivacidad;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.exceptions.AccountDisabledException;
import com.upc.grupo3.exceptions.InvalidPrivacyPreferencesRequestException;
import com.upc.grupo3.exceptions.LocationPrivacyDisabledException;
import com.upc.grupo3.exceptions.ResourceNotFoundException;
import com.upc.grupo3.exceptions.UnauthenticatedUserException;
import com.upc.grupo3.repositories.ConfiguracionPrivacidadRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivacyPreferencesService {

    private static final boolean DEFAULT_REAL_TIME_LOCATION_ENABLED = true;
    private static final boolean DEFAULT_PERSONAL_DATA_SHARING_ENABLED = false;

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionPrivacidadRepository configuracionPrivacidadRepository;

    @Transactional
    public PrivacyPreferencesResponseDTO getPreferences(String authenticatedEmail) {
        Usuario usuario = resolveActiveAuthenticatedUser(authenticatedEmail);
        ConfiguracionPrivacidad configuracionPrivacidad = findOrCreateConfiguration(usuario);

        return buildResponse(
                usuario.getIdUsuario(),
                configuracionPrivacidad,
                "Preferencias de privacidad obtenidas correctamente");
    }

    @Transactional
    public PrivacyPreferencesResponseDTO updatePreferences(
            String authenticatedEmail,
            PrivacyPreferencesRequestDTO request) {
        Usuario usuario = resolveActiveAuthenticatedUser(authenticatedEmail);
        ConfiguracionPrivacidad configuracionPrivacidad = findOrCreateConfiguration(usuario);

        configuracionPrivacidad.setUbicacionTiempoReal(request.getRealTimeLocationEnabled());
        configuracionPrivacidad.setCompartirDatosPersonales(request.getPersonalDataSharingEnabled());
        configuracionPrivacidad.setFechaActualizacion(LocalDateTime.now());

        ConfiguracionPrivacidad configuracionActualizada =
                configuracionPrivacidadRepository.save(configuracionPrivacidad);
        log.info("Preferencias de privacidad actualizadas userId={} ubicacionTiempoReal={} compartirDatosPersonales={}",
                usuario.getIdUsuario(),
                configuracionActualizada.getUbicacionTiempoReal(),
                configuracionActualizada.getCompartirDatosPersonales());

        return buildResponse(
                usuario.getIdUsuario(),
                configuracionActualizada,
                "Preferencias de privacidad actualizadas correctamente");
    }

    @Transactional
    public void validateRealTimeLocationUsage(Integer userId) {
        if (userId == null || userId <= 0) {
            log.warn("Validacion de ubicacion rechazada por identificador de usuario invalido userId={}", userId);
            throw new InvalidPrivacyPreferencesRequestException(
                    "El identificador del usuario es obligatorio y debe ser mayor que cero");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Validacion de ubicacion rechazada porque el usuario no existe userId={}", userId);
                    return new ResourceNotFoundException("No se encontro el usuario");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Validacion de ubicacion rechazada por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        ConfiguracionPrivacidad configuracionPrivacidad = findOrCreateConfiguration(usuario);
        if (!Boolean.TRUE.equals(configuracionPrivacidad.getUbicacionTiempoReal())) {
            log.warn("Procesamiento de ubicacion bloqueado por configuracion de privacidad userId={}",
                    usuario.getIdUsuario());
            throw new LocationPrivacyDisabledException(
                    "La configuracion de privacidad del usuario no permite usar la ubicacion en tiempo real");
        }
    }

    private Usuario resolveActiveAuthenticatedUser(String authenticatedEmail) {
        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Operacion de privacidad rechazada por falta de sesion autenticada");
            throw new UnauthenticatedUserException("No existe una sesion autenticada para gestionar la privacidad");
        }

        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Usuario autenticado no encontrado durante gestion de privacidad email={}",
                            normalizedEmail);
                    return new ResourceNotFoundException("No se encontro el usuario autenticado");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Operacion de privacidad rechazada por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        return usuario;
    }

    private ConfiguracionPrivacidad findOrCreateConfiguration(Usuario usuario) {
        return configuracionPrivacidadRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> createDefaultConfiguration(usuario));
    }

    private ConfiguracionPrivacidad createDefaultConfiguration(Usuario usuario) {
        ConfiguracionPrivacidad configuracionPrivacidad = ConfiguracionPrivacidad.builder()
                .usuario(usuario)
                .ubicacionTiempoReal(DEFAULT_REAL_TIME_LOCATION_ENABLED)
                .compartirDatosPersonales(DEFAULT_PERSONAL_DATA_SHARING_ENABLED)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        ConfiguracionPrivacidad configuracionCreada = configuracionPrivacidadRepository.save(configuracionPrivacidad);
        log.info("Configuracion de privacidad por defecto creada userId={} ubicacionTiempoReal={} compartirDatosPersonales={}",
                usuario.getIdUsuario(),
                configuracionCreada.getUbicacionTiempoReal(),
                configuracionCreada.getCompartirDatosPersonales());
        return configuracionCreada;
    }

    private PrivacyPreferencesResponseDTO buildResponse(
            Integer userId,
            ConfiguracionPrivacidad configuracionPrivacidad,
            String message) {
        return PrivacyPreferencesResponseDTO.builder()
                .userId(userId)
                .realTimeLocationEnabled(configuracionPrivacidad.getUbicacionTiempoReal())
                .personalDataSharingEnabled(configuracionPrivacidad.getCompartirDatosPersonales())
                .message(message)
                .build();
    }
}
