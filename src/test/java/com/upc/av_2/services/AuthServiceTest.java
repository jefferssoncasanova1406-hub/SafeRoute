package com.upc.av_2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upc.av_2.dtos.LoginRequestDTO;
import com.upc.av_2.dtos.LoginResponseDTO;
import com.upc.av_2.dtos.LogoutRequestDTO;
import com.upc.av_2.dtos.LogoutResponseDTO;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.InvalidAuthorizationHeaderException;
import com.upc.av_2.exceptions.InvalidCredentialsException;
import com.upc.av_2.exceptions.TokenOwnershipException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.ConfiguracionPrivacidadRepository;
import com.upc.av_2.repositories.PerfilRepository;
import com.upc.av_2.repositories.RolRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.security.JwtService;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ConfiguracionPrivacidadRepository configuracionPrivacidadRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValidAndAccountIsActive() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("luis.rojas@demo.com")
                .password("12345678")
                .build();
        Rol rol = Rol.builder()
                .idRol(2)
                .nombre("usuario")
                .descripcion("Usuario registrado")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rol(rol)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("12345678", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken(usuario, "usuario")).thenReturn("jwt-token");

        LoginResponseDTO response = authService.login(request);

        assertEquals("Inicio de sesion exitoso", response.getMessage());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(2, response.getUser().getId());
        assertEquals("usuario", response.getUser().getRol());
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("luis.rojas@demo.com")
                .password("incorrecta")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .estado(Boolean.TRUE)
                .rol(Rol.builder().idRol(2).nombre("usuario").build())
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "$2a$10$hash")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request));

        assertEquals("Credenciales invalidas", exception.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void loginShouldThrowAccountDisabledWhenUserIsSuspended() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carla.vega@demo.com")
                .password("12345678")
                .build();
        Usuario usuario = Usuario.builder()
                .idUsuario(3)
                .email("carla.vega@demo.com")
                .contrasena("$2a$10$hash")
                .estado(Boolean.FALSE)
                .rol(Rol.builder().idRol(2).nombre("usuario").build())
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("carla.vega@demo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("12345678", "$2a$10$hash")).thenReturn(true);

        AccountDisabledException exception = assertThrows(
                AccountDisabledException.class,
                () -> authService.login(request));

        assertEquals("La cuenta no se encuentra habilitada", exception.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void logoutShouldRevokeTokenAndReturnSuccessMessage() {
        LogoutRequestDTO request = new LogoutRequestDTO();
        LocalDateTime expirationAt = LocalDateTime.of(2026, 5, 12, 10, 30);
        java.util.Date expirationDate = java.util.Date.from(expirationAt.atZone(java.time.ZoneId.systemDefault()).toInstant());

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(tokenRevocationService.isTokenRevoked("jwt-token")).thenReturn(false);
        when(jwtService.extractUsername("jwt-token")).thenReturn("luis.rojas@demo.com");
        when(jwtService.extractExpiration("jwt-token")).thenReturn(expirationDate);

        LogoutResponseDTO response = authService.logout("Bearer jwt-token", request, "luis.rojas@demo.com");

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals("Sesion cerrada correctamente", response.getMessage());
        verify(tokenRevocationService).revokeToken("jwt-token", "luis.rojas@demo.com", expirationAt);
    }

    @Test
    void logoutShouldThrowUnauthenticatedUserExceptionWhenAuthenticatedUserIsMissing() {
        LogoutRequestDTO request = new LogoutRequestDTO();

        UnauthenticatedUserException exception = assertThrows(
                UnauthenticatedUserException.class,
                () -> authService.logout("Bearer jwt-token", request, null));

        assertEquals("No existe una sesion autenticada para cerrar", exception.getMessage());
    }

    @Test
    void logoutShouldThrowInvalidAuthorizationHeaderExceptionWhenHeaderIsInvalid() {
        LogoutRequestDTO request = new LogoutRequestDTO();

        InvalidAuthorizationHeaderException exception = assertThrows(
                InvalidAuthorizationHeaderException.class,
                () -> authService.logout("Token jwt-token", request, "luis.rojas@demo.com"));

        assertEquals("El encabezado Authorization debe usar el esquema Bearer", exception.getMessage());
    }

    @Test
    void logoutShouldThrowInvalidAuthorizationHeaderExceptionWhenTokenWasAlreadyRevoked() {
        LogoutRequestDTO request = new LogoutRequestDTO();

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(tokenRevocationService.isTokenRevoked("jwt-token")).thenReturn(true);

        InvalidAuthorizationHeaderException exception = assertThrows(
                InvalidAuthorizationHeaderException.class,
                () -> authService.logout("Bearer jwt-token", request, "luis.rojas@demo.com"));

        assertEquals("La sesion ya fue cerrada", exception.getMessage());
    }

    @Test
    void logoutShouldThrowTokenOwnershipExceptionWhenTokenBelongsToAnotherUser() {
        LogoutRequestDTO request = new LogoutRequestDTO();

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(tokenRevocationService.isTokenRevoked("jwt-token")).thenReturn(false);
        when(jwtService.extractUsername("jwt-token")).thenReturn("otro.usuario@demo.com");

        TokenOwnershipException exception = assertThrows(
                TokenOwnershipException.class,
                () -> authService.logout("Bearer jwt-token", request, "luis.rojas@demo.com"));

        assertEquals("El token no pertenece al usuario autenticado", exception.getMessage());
    }
}
