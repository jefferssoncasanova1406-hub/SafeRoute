package com.upc.av_2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.av_2.dtos.RiskZoneGeometryDTO;
import com.upc.av_2.dtos.RiskZoneListResponseDTO;
import com.upc.av_2.dtos.RiskZoneLocationDTO;
import com.upc.av_2.dtos.RiskZoneOperationResponseDTO;
import com.upc.av_2.dtos.RiskZoneRequestDTO;
import com.upc.av_2.entidades.Rol;
import com.upc.av_2.entidades.Ubicacion;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.entidades.ZonaRiesgo;
import com.upc.av_2.exceptions.InvalidRiskZoneRequestException;
import com.upc.av_2.exceptions.ResourceNotFoundException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
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
class RiskZoneServiceTest {

    @Mock
    private ZonaRiesgoRepository zonaRiesgoRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private RiskZoneService riskZoneService;

    @BeforeEach
    void setUp() {
        riskZoneService = new RiskZoneService(
                zonaRiesgoRepository,
                ubicacionRepository,
                usuarioRepository,
                new GeometryJsonService(new ObjectMapper()),
                new RiskLevelCatalog());
    }

    @Test
    void createRiskZoneShouldPersistZoneAndLocation() {
        Usuario admin = buildAdminUser();
        RiskZoneRequestDTO request = buildRiskZoneRequest();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));
        when(ubicacionRepository.save(any(Ubicacion.class))).thenAnswer(invocation -> {
            Ubicacion ubicacion = invocation.getArgument(0);
            ubicacion.setIdUbicacion(10);
            return ubicacion;
        });
        when(zonaRiesgoRepository.save(any(ZonaRiesgo.class))).thenAnswer(invocation -> {
            ZonaRiesgo zonaRiesgo = invocation.getArgument(0);
            zonaRiesgo.setIdZona(5);
            return zonaRiesgo;
        });

        RiskZoneOperationResponseDTO response =
                riskZoneService.createRiskZone("ana.torres@demo.com", request);

        assertEquals("Zona de riesgo creada correctamente", response.getMessage());
        assertEquals(5, response.getZona().getIdZona());
        assertEquals("ROBO", response.getZona().getTipo());
        assertEquals("ACTIVA", response.getZona().getEstado());
        assertEquals(3, response.getZona().getNivelRiesgo());
        verify(ubicacionRepository).save(any(Ubicacion.class));
        verify(zonaRiesgoRepository).save(any(ZonaRiesgo.class));
    }

    @Test
    void getRiskZonesShouldFilterByEstadoAndNivelRiesgo() {
        Usuario admin = buildAdminUser();
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(10)
                .latitud(new BigDecimal("-12.0464000"))
                .longitud(new BigDecimal("-77.0428000"))
                .distrito("Cercado de Lima")
                .ciudad("Lima")
                .build();
        ZonaRiesgo zonaRiesgo = ZonaRiesgo.builder()
                .idZona(5)
                .tipo("ROBO")
                .nivelRiesgo(3)
                .descripcion("Zona con alta incidencia reportada")
                .estado(Boolean.TRUE)
                .coordenadasGeojson("{\"type\":\"Polygon\",\"coordinates\":[[[-77.0432,-12.0469],[-77.0423,-12.0469],[-77.0423,-12.0460],[-77.0432,-12.0460],[-77.0432,-12.0469]]]}" )
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 10, 0))
                .ubicacion(ubicacion)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));
        when(zonaRiesgoRepository.findByEstadoAndNivelRiesgo(Boolean.TRUE, 3)).thenReturn(List.of(zonaRiesgo));
        when(ubicacionRepository.findAllById(List.of(10))).thenReturn(List.of(ubicacion));

        RiskZoneListResponseDTO response =
                riskZoneService.getRiskZones("ana.torres@demo.com", "ACTIVA", 3);

        assertEquals("Zonas de riesgo obtenidas correctamente", response.getMessage());
        assertEquals(1, response.getZonas().size());
        assertEquals("ACTIVA", response.getZonas().get(0).getEstado());
        assertEquals("Lima", response.getZonas().get(0).getUbicacion().getCiudad());
    }

    @Test
    void deactivateRiskZoneShouldMarkZoneAsInactive() {
        Usuario admin = buildAdminUser();
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(10)
                .latitud(new BigDecimal("-12.0464000"))
                .longitud(new BigDecimal("-77.0428000"))
                .distrito("Cercado de Lima")
                .ciudad("Lima")
                .build();
        ZonaRiesgo zonaRiesgo = ZonaRiesgo.builder()
                .idZona(5)
                .tipo("ROBO")
                .nivelRiesgo(3)
                .descripcion("Zona con alta incidencia reportada")
                .estado(Boolean.TRUE)
                .coordenadasGeojson("{\"type\":\"Polygon\",\"coordinates\":[[[-77.0432,-12.0469],[-77.0423,-12.0469],[-77.0423,-12.0460],[-77.0432,-12.0460],[-77.0432,-12.0469]]]}" )
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 10, 0))
                .ubicacion(ubicacion)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));
        when(zonaRiesgoRepository.findById(5)).thenReturn(Optional.of(zonaRiesgo));
        when(zonaRiesgoRepository.save(any(ZonaRiesgo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RiskZoneOperationResponseDTO response =
                riskZoneService.deactivateRiskZone("ana.torres@demo.com", 5);

        assertEquals("Zona de riesgo desactivada correctamente", response.getMessage());
        assertEquals("INACTIVA", response.getZona().getEstado());
        assertFalse(response.getZona().getEstado().equals("ACTIVA"));
    }

    @Test
    void createRiskZoneShouldThrowWhenGeometryIsNotClosed() {
        Usuario admin = buildAdminUser();
        RiskZoneRequestDTO request = buildRiskZoneRequest();
        request.getGeometria().setCoordinates(List.of(List.of(
                List.of(new BigDecimal("-77.0432"), new BigDecimal("-12.0469")),
                List.of(new BigDecimal("-77.0423"), new BigDecimal("-12.0469")),
                List.of(new BigDecimal("-77.0423"), new BigDecimal("-12.0460")),
                List.of(new BigDecimal("-77.0432"), new BigDecimal("-12.0460"))
        )));

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));

        InvalidRiskZoneRequestException exception = assertThrows(
                InvalidRiskZoneRequestException.class,
                () -> riskZoneService.createRiskZone("ana.torres@demo.com", request));

        assertEquals("El primer y el ultimo punto del poligono deben ser iguales", exception.getMessage());
    }

    @Test
    void getRiskZonesShouldThrowWhenThereIsNoAuthenticatedSession() {
        UnauthenticatedUserException exception = assertThrows(
                UnauthenticatedUserException.class,
                () -> riskZoneService.getRiskZones(null, null, null));

        assertEquals("No existe una sesion autenticada para gestionar zonas de riesgo", exception.getMessage());
    }

    @Test
    void getRiskZonesShouldThrowWhenEstadoFilterIsInvalid() {
        Usuario admin = buildAdminUser();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));

        InvalidRiskZoneRequestException exception = assertThrows(
                InvalidRiskZoneRequestException.class,
                () -> riskZoneService.getRiskZones("ana.torres@demo.com", "PENDIENTE", null));

        assertEquals("El estado debe ser ACTIVA o INACTIVA", exception.getMessage());
    }

    @Test
    void updateRiskZoneShouldThrowWhenZoneDoesNotExist() {
        Usuario admin = buildAdminUser();
        RiskZoneRequestDTO request = buildRiskZoneRequest();

        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com")).thenReturn(Optional.of(admin));
        when(zonaRiesgoRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> riskZoneService.updateRiskZone("ana.torres@demo.com", 99, request));

        assertEquals("No se encontro la zona de riesgo", exception.getMessage());
    }

    private Usuario buildAdminUser() {
        return Usuario.builder()
                .idUsuario(1)
                .nombre("Ana Torres")
                .email("ana.torres@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 10))
                .estado(Boolean.TRUE)
                .rol(Rol.builder().idRol(1).nombre("admin").build())
                .build();
    }

    private RiskZoneRequestDTO buildRiskZoneRequest() {
        return RiskZoneRequestDTO.builder()
                .tipo("ROBO")
                .nivelRiesgo(3)
                .descripcion("Zona con alta incidencia reportada")
                .geometria(RiskZoneGeometryDTO.builder()
                        .type("Polygon")
                        .coordinates(List.of(List.of(
                                List.of(new BigDecimal("-77.0432"), new BigDecimal("-12.0469")),
                                List.of(new BigDecimal("-77.0423"), new BigDecimal("-12.0469")),
                                List.of(new BigDecimal("-77.0423"), new BigDecimal("-12.0460")),
                                List.of(new BigDecimal("-77.0432"), new BigDecimal("-12.0460")),
                                List.of(new BigDecimal("-77.0432"), new BigDecimal("-12.0469"))
                        )))
                        .build())
                .ubicacion(RiskZoneLocationDTO.builder()
                        .latitud(new BigDecimal("-12.0464000"))
                        .longitud(new BigDecimal("-77.0428000"))
                        .distrito("Cercado de Lima")
                        .ciudad("Lima")
                        .build())
                .build();
    }
}
