package com.upc.grupo3.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.upc.grupo3.dtos.common.AdminAccessValidationRequestDTO;
import com.upc.grupo3.dtos.common.AdminAccessValidationResponseDTO;
import com.upc.grupo3.entidades.Rol;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.exceptions.InvalidAdminAccessRequestException;
import com.upc.grupo3.exceptions.UnauthenticatedUserException;
import com.upc.grupo3.repositories.RolRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAccessServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private AdminAccessService adminAccessService;

    @Test
    void validateAdministrativeAccessShouldAuthorizeAdminUser() {
        AdminAccessValidationRequestDTO request = AdminAccessValidationRequestDTO.builder()
                .resource("user-management")
                .action("read")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(1)
                .nombre("Ana Torres")
                .email("ana.torres@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 10))
                .estado(Boolean.TRUE)
                .rol(Rol.builder()
                        .idRol(1)
                        .nombre("admin")
                        .descripcion("Administrador del sistema")
                        .build())
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(usuario));

        AdminAccessValidationResponseDTO response =
                adminAccessService.validateAdministrativeAccess(request, "ana.torres@demo.com");

        assertTrue(response.getAuthorized());
        assertEquals("ADMIN", response.getRole());
        assertEquals("user-management", response.getResource());
        assertEquals("READ", response.getAction());
        assertEquals("Acceso autorizado", response.getMessage());
    }

    @Test
    void validateAdministrativeAccessShouldRejectCommonUser() {
        AdminAccessValidationRequestDTO request = AdminAccessValidationRequestDTO.builder()
                .resource("user-management")
                .action("update")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rol(Rol.builder()
                        .idRol(2)
                        .nombre("usuario")
                        .descripcion("Usuario registrado")
                        .build())
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));

        AdminAccessValidationResponseDTO response =
                adminAccessService.validateAdministrativeAccess(request, "luis.rojas@demo.com");

        assertFalse(response.getAuthorized());
        assertEquals("USUARIO", response.getRole());
        assertEquals("user-management", response.getResource());
        assertEquals("UPDATE", response.getAction());
        assertEquals("Acceso no autorizado", response.getMessage());
    }

    @Test
    void validateAdministrativeAccessShouldThrowWhenThereIsNoAuthenticatedSession() {
        AdminAccessValidationRequestDTO request = AdminAccessValidationRequestDTO.builder()
                .resource("user-management")
                .action("read")
                .build();

        UnauthenticatedUserException exception = assertThrows(
                UnauthenticatedUserException.class,
                () -> adminAccessService.validateAdministrativeAccess(request, null));

        assertEquals("No existe una sesion autenticada para validar el acceso", exception.getMessage());
    }

    @Test
    void validateAdministrativeAccessShouldThrowWhenActionIsNotSupported() {
        AdminAccessValidationRequestDTO request = AdminAccessValidationRequestDTO.builder()
                .resource("user-management")
                .action("approve")
                .build();

        InvalidAdminAccessRequestException exception = assertThrows(
                InvalidAdminAccessRequestException.class,
                () -> adminAccessService.validateAdministrativeAccess(request, "ana.torres@demo.com"));

        assertEquals("La accion debe ser una de las siguientes: READ, CREATE, UPDATE o DELETE",
                exception.getMessage());
    }

    @Test
    void validateAdministrativeAccessShouldThrowWhenResourceContainsInvalidCharacters() {
        AdminAccessValidationRequestDTO request = AdminAccessValidationRequestDTO.builder()
                .resource("user management")
                .action("read")
                .build();

        InvalidAdminAccessRequestException exception = assertThrows(
                InvalidAdminAccessRequestException.class,
                () -> adminAccessService.validateAdministrativeAccess(request, "ana.torres@demo.com"));

        assertEquals("El recurso solo puede contener letras, numeros, guiones y guion bajo",
                exception.getMessage());
    }
}
