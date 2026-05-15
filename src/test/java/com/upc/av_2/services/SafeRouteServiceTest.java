package com.upc.av_2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.av_2.dtos.SafeRoutePointDTO;
import com.upc.av_2.dtos.SafeRouteRequestDTO;
import com.upc.av_2.dtos.SafeRouteResponseDTO;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Ruta;
import com.upc.av_2.entidades.Ubicacion;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.entidades.ZonaRiesgo;
import com.upc.av_2.exceptions.GeographicDataNotAvailableException;
import com.upc.av_2.exceptions.InvalidSafeRouteRequestException;
import com.upc.av_2.repositories.RutaRepository;
import com.upc.av_2.repositories.RutaZonaRepository;
import com.upc.av_2.repositories.UbicacionRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.repositories.ZonaRiesgoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SafeRouteServiceTest {

    @Mock
    private RutaRepository rutaRepository;

    @Mock
    private RutaZonaRepository rutaZonaRepository;

    @Mock
    private ZonaRiesgoRepository zonaRiesgoRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private SafeRouteService safeRouteService;

    @BeforeEach
    void setUp() {
        safeRouteService = new SafeRouteService(
                rutaRepository,
                rutaZonaRepository,
                zonaRiesgoRepository,
                ubicacionRepository,
                usuarioRepository,
                new GeometryJsonService(new ObjectMapper()),
                new RiskLevelCatalog());
    }

    @Test
    void calculateSafeRouteShouldPersistRouteAndDetectCrossedRiskZones() {
        Usuario user = buildActiveUser();
        Ubicacion location = Ubicacion.builder()
                .idUbicacion(10)
                .latitud(new BigDecimal("-12.0464000"))
                .longitud(new BigDecimal("-77.0428000"))
                .distrito("Cercado de Lima")
                .ciudad("Lima")
                .build();
        ZonaRiesgo riskZone = ZonaRiesgo.builder()
                .idZona(5)
                .tipo("ROBO")
                .nivelRiesgo(3)
                .descripcion("Zona con alta incidencia reportada")
                .estado(Boolean.TRUE)
                .coordenadasGeojson(
                        "{\"type\":\"Polygon\",\"coordinates\":[[[-77.0432000,-12.0469000],[-77.0423000,-12.0469000],[-77.0423000,-12.0460000],[-77.0432000,-12.0460000],[-77.0432000,-12.0469000]]]}"
                )
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 9, 0))
                .ubicacion(location)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));
        when(ubicacionRepository.count()).thenReturn(2L);
        when(zonaRiesgoRepository.findByEstadoTrueOrderByNivelRiesgoDescFechaActualizacionDesc())
                .thenReturn(List.of(riskZone));
        when(ubicacionRepository.findAllById(List.of(10))).thenReturn(List.of(location));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocation -> {
            Ruta route = invocation.getArgument(0);
            route.setIdRuta(15);
            return route;
        });

        SafeRouteResponseDTO response = safeRouteService.calculateSafeRoute(
                "luis.rojas@demo.com",
                SafeRouteRequestDTO.builder()
                        .origen(SafeRoutePointDTO.builder()
                                .latitud(new BigDecimal("-12.0464000"))
                                .longitud(new BigDecimal("-77.0428000"))
                                .referencia("Av. Abancay 123")
                                .distrito("Cercado de Lima")
                                .ciudad("Lima")
                                .build())
                        .destino(SafeRoutePointDTO.builder()
                                .latitud(new BigDecimal("-12.1211000"))
                                .longitud(new BigDecimal("-77.0305000"))
                                .referencia("Parque Kennedy")
                                .distrito("Miraflores")
                                .ciudad("Lima")
                                .build())
                        .build());

        assertEquals(15, response.getRutaId());
        assertTrue(response.getCruzaZonasRiesgo());
        assertEquals(3, response.getNivelRiesgo());
        assertEquals("alto", response.getNivelRiesgoNombre());
        assertEquals(1, response.getZonasRiesgo().size());
        assertEquals("ROBO", response.getZonasRiesgo().get(0).getTipo());
        verify(rutaRepository).save(any(Ruta.class));
        verify(rutaZonaRepository).saveAll(any());
    }

    @Test
    void calculateSafeRouteShouldReturnLowRiskWhenThereAreNoActiveZones() {
        Usuario user = buildActiveUser();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));
        when(ubicacionRepository.count()).thenReturn(2L);
        when(zonaRiesgoRepository.findByEstadoTrueOrderByNivelRiesgoDescFechaActualizacionDesc())
                .thenReturn(List.of());
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(invocation -> {
            Ruta route = invocation.getArgument(0);
            route.setIdRuta(20);
            return route;
        });

        SafeRouteResponseDTO response = safeRouteService.calculateSafeRoute(
                "luis.rojas@demo.com",
                SafeRouteRequestDTO.builder()
                        .origen(SafeRoutePointDTO.builder()
                                .latitud(new BigDecimal("-12.0500000"))
                                .longitud(new BigDecimal("-77.0400000"))
                                .build())
                        .destino(SafeRoutePointDTO.builder()
                                .latitud(new BigDecimal("-12.1200000"))
                                .longitud(new BigDecimal("-77.0200000"))
                                .build())
                        .build());

        assertEquals(20, response.getRutaId());
        assertFalse(response.getCruzaZonasRiesgo());
        assertEquals(1, response.getNivelRiesgo());
        assertEquals("bajo", response.getNivelRiesgoNombre());
        assertEquals(0, response.getZonasRiesgo().size());
    }

    @Test
    void calculateSafeRouteShouldThrowWhenOriginOrDestinationAreEqual() {
        Usuario user = buildActiveUser();
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));

        InvalidSafeRouteRequestException exception = assertThrows(
                InvalidSafeRouteRequestException.class,
                () -> safeRouteService.calculateSafeRoute(
                        "luis.rojas@demo.com",
                        SafeRouteRequestDTO.builder()
                                .origen(SafeRoutePointDTO.builder()
                                        .latitud(new BigDecimal("-12.0464000"))
                                        .longitud(new BigDecimal("-77.0428000"))
                                        .build())
                                .destino(SafeRoutePointDTO.builder()
                                        .latitud(new BigDecimal("-12.0464000"))
                                        .longitud(new BigDecimal("-77.0428000"))
                                        .build())
                                .build()));

        assertEquals("El origen y el destino no pueden ser iguales", exception.getMessage());
    }

    @Test
    void calculateSafeRouteShouldThrowWhenThereIsNoGeographicData() {
        Usuario user = buildActiveUser();
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));
        when(ubicacionRepository.count()).thenReturn(0L);

        GeographicDataNotAvailableException exception = assertThrows(
                GeographicDataNotAvailableException.class,
                () -> safeRouteService.calculateSafeRoute(
                        "luis.rojas@demo.com",
                        SafeRouteRequestDTO.builder()
                                .origen(SafeRoutePointDTO.builder()
                                        .latitud(new BigDecimal("-12.0464000"))
                                        .longitud(new BigDecimal("-77.0428000"))
                                        .build())
                                .destino(SafeRoutePointDTO.builder()
                                        .latitud(new BigDecimal("-12.1211000"))
                                        .longitud(new BigDecimal("-77.0305000"))
                                        .build())
                                .build()));

        assertEquals("No existen datos geograficos disponibles para calcular la ruta", exception.getMessage());
    }

    private Usuario buildActiveUser() {
        return Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rol(Rol.builder().idRol(2).nombre("usuario").build())
                .build();
    }
}
