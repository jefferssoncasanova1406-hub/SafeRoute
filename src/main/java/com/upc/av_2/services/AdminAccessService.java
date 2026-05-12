package com.upc.av_2.services;

import com.upc.av_2.dtos.AdminAccessValidationRequestDTO;
import com.upc.av_2.dtos.AdminAccessValidationResponseDTO;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.InvalidAdminAccessRequestException;
import com.upc.av_2.exceptions.ResourceNotFoundException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.RolRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import java.util.Set;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAccessService {

    private static final String ADMIN_ROLE_NAME = "admin";
    private static final Set<String> ALLOWED_ACTIONS = Set.of("READ", "CREATE", "UPDATE", "DELETE");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Transactional(readOnly = true)
    public AdminAccessValidationResponseDTO validateAdministrativeAccess(
            AdminAccessValidationRequestDTO request,
            String authenticatedEmail) {
        String normalizedResource = normalizeResource(request.getResource());
        String normalizedAction = normalizeAction(request.getAction());

        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Validacion de acceso administrativo rechazada por falta de sesion autenticada resource={} action={}",
                    normalizedResource, normalizedAction);
            throw new UnauthenticatedUserException("No existe una sesion autenticada para validar el acceso");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> {
                    log.warn("Usuario autenticado no encontrado durante validacion administrativa email={}",
                            authenticatedEmail);
                    return new ResourceNotFoundException("No se encontro el usuario autenticado");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Validacion administrativa rechazada por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        Rol rol = rolRepository.findById(usuario.getRolIdRol())
                .orElseThrow(() -> new ApplicationConfigurationException(
                        "No se encontro el rol asociado al usuario con id " + usuario.getIdUsuario()));

        boolean authorized = ADMIN_ROLE_NAME.equalsIgnoreCase(rol.getNombre());
        String message = authorized ? "Acceso autorizado" : "Acceso no autorizado";
        if (authorized) {
            log.info("Resultado de validacion administrativa userId={} email={} role={} resource={} action={} authorized=true",
                    usuario.getIdUsuario(), usuario.getEmail(), rol.getNombre(), normalizedResource, normalizedAction);
        } else {
            log.warn("Acceso administrativo denegado userId={} email={} role={} resource={} action={}",
                    usuario.getIdUsuario(), usuario.getEmail(), rol.getNombre(), normalizedResource, normalizedAction);
        }

        return AdminAccessValidationResponseDTO.builder()
                .authorized(authorized)
                .role(rol.getNombre().toUpperCase(Locale.ROOT))
                .resource(normalizedResource)
                .action(normalizedAction)
                .message(message)
                .build();
    }

    private String normalizeResource(String resource) {
        if (!StringUtils.hasText(resource)) {
            throw new InvalidAdminAccessRequestException("El recurso es obligatorio");
        }

        String normalizedResource = resource.trim();
        if (!normalizedResource.matches("[A-Za-z0-9_-]+")) {
            log.warn("Solicitud administrativa rechazada por recurso invalido resource={}", normalizedResource);
            throw new InvalidAdminAccessRequestException(
                    "El recurso solo puede contener letras, numeros, guiones y guion bajo");
        }
        return normalizedResource;
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            throw new InvalidAdminAccessRequestException("La accion es obligatoria");
        }

        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(normalizedAction)) {
            log.warn("Solicitud administrativa rechazada por accion invalida action={}", normalizedAction);
            throw new InvalidAdminAccessRequestException(
                    "La accion debe ser una de las siguientes: READ, CREATE, UPDATE o DELETE");
        }
        return normalizedAction;
    }
}
