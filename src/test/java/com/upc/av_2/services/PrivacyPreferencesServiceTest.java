package com.upc.av_2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upc.av_2.dtos.PrivacyPreferencesRequestDTO;
import com.upc.av_2.dtos.PrivacyPreferencesResponseDTO;
import com.upc.av_2.entidades.ConfiguracionPrivacidad;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.InvalidPrivacyPreferencesRequestException;
import com.upc.av_2.exceptions.LocationPrivacyDisabledException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.ConfiguracionPrivacidadRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyPreferencesServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionPrivacidadRepository configuracionPrivacidadRepository;

    @InjectMocks
    private PrivacyPreferencesService privacyPreferencesService;

    @Test
    void getPreferencesShouldCreateDefaultConfigurationWhenMissing() {
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rolIdRol(2)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(configuracionPrivacidadRepository.findByIdUsuario(2)).thenReturn(Optional.empty());
        when(configuracionPrivacidadRepository.save(any(ConfiguracionPrivacidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrivacyPreferencesResponseDTO response =
                privacyPreferencesService.getPreferences("luis.rojas@demo.com");

        assertEquals(2, response.getUserId());
        assertTrue(response.getRealTimeLocationEnabled());
        assertFalse(response.getPersonalDataSharingEnabled());
        assertEquals("Preferencias de privacidad obtenidas correctamente", response.getMessage());
        verify(configuracionPrivacidadRepository).save(any(ConfiguracionPrivacidad.class));
    }

    @Test
    void updatePreferencesShouldPersistProvidedValues() {
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rolIdRol(2)
                .build();
        ConfiguracionPrivacidad configuracionPrivacidad = ConfiguracionPrivacidad.builder()
                .idConfiguracionPrivacidad(10)
                .idUsuario(2)
                .ubicacionTiempoReal(Boolean.TRUE)
                .compartirDatosPersonales(Boolean.FALSE)
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 10, 0))
                .build();
        PrivacyPreferencesRequestDTO request = PrivacyPreferencesRequestDTO.builder()
                .realTimeLocationEnabled(Boolean.FALSE)
                .personalDataSharingEnabled(Boolean.TRUE)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(configuracionPrivacidadRepository.findByIdUsuario(2))
                .thenReturn(Optional.of(configuracionPrivacidad));
        when(configuracionPrivacidadRepository.save(any(ConfiguracionPrivacidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrivacyPreferencesResponseDTO response =
                privacyPreferencesService.updatePreferences("luis.rojas@demo.com", request);

        assertEquals(2, response.getUserId());
        assertFalse(response.getRealTimeLocationEnabled());
        assertTrue(response.getPersonalDataSharingEnabled());
        assertEquals("Preferencias de privacidad actualizadas correctamente", response.getMessage());
    }

    @Test
    void getPreferencesShouldThrowWhenThereIsNoAuthenticatedSession() {
        UnauthenticatedUserException exception = assertThrows(
                UnauthenticatedUserException.class,
                () -> privacyPreferencesService.getPreferences(null));

        assertEquals("No existe una sesion autenticada para gestionar la privacidad", exception.getMessage());
    }

    @Test
    void getPreferencesShouldThrowWhenAccountIsDisabled() {
        Usuario usuario = Usuario.builder()
                .idUsuario(3)
                .nombre("Carla Vega")
                .email("carla.vega@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 2, 1))
                .estado(Boolean.FALSE)
                .rolIdRol(2)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("carla.vega@demo.com")).thenReturn(Optional.of(usuario));

        AccountDisabledException exception = assertThrows(
                AccountDisabledException.class,
                () -> privacyPreferencesService.getPreferences("carla.vega@demo.com"));

        assertEquals("La cuenta no se encuentra habilitada", exception.getMessage());
    }

    @Test
    void validateRealTimeLocationUsageShouldThrowWhenPrivacyDisablesLocation() {
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rolIdRol(2)
                .build();
        ConfiguracionPrivacidad configuracionPrivacidad = ConfiguracionPrivacidad.builder()
                .idConfiguracionPrivacidad(10)
                .idUsuario(2)
                .ubicacionTiempoReal(Boolean.FALSE)
                .compartirDatosPersonales(Boolean.TRUE)
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 10, 0))
                .build();

        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));
        when(configuracionPrivacidadRepository.findByIdUsuario(2))
                .thenReturn(Optional.of(configuracionPrivacidad));

        LocationPrivacyDisabledException exception = assertThrows(
                LocationPrivacyDisabledException.class,
                () -> privacyPreferencesService.validateRealTimeLocationUsage(2));

        assertEquals(
                "La configuracion de privacidad del usuario no permite usar la ubicacion en tiempo real",
                exception.getMessage());
    }

    @Test
    void validateRealTimeLocationUsageShouldThrowWhenUserIdIsInvalid() {
        InvalidPrivacyPreferencesRequestException exception = assertThrows(
                InvalidPrivacyPreferencesRequestException.class,
                () -> privacyPreferencesService.validateRealTimeLocationUsage(0));

        assertEquals(
                "El identificador del usuario es obligatorio y debe ser mayor que cero",
                exception.getMessage());
    }
}
