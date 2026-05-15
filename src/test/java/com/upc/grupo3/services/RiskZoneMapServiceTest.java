package com.upc.grupo3.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapRequestDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapResponseDTO;
import com.upc.grupo3.entidades.Rol;
import com.upc.grupo3.entidades.Ubicacion;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.entidades.ZonaRiesgo;
import com.upc.grupo3.exceptions.InvalidRiskZoneRequestException;
import com.upc.grupo3.repositories.UbicacionRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.repositories.ZonaRiesgoRepository;
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
class RiskZoneMapServiceTest {

    @Mock
    private ZonaRiesgoRepository zonaRiesgoRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private RiskZoneMapService riskZoneMapService;

    @BeforeEach
    void setUp() {
        riskZoneMapService = new RiskZoneMapService(
                zonaRiesgoRepository,
                ubicacionRepository,
                usuarioRepository,
                new GeometryJsonService(new ObjectMapper()),
                new RiskLevelCatalog());
    }

    @Test
    void getActiveRiskZonesForMapShouldReturnActiveZonesForDistrict() {
        Usuario user = buildActiveUser();
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(10)
                .latitud(new BigDecimal("-12.1211000"))
                .longitud(new BigDecimal("-77.0305000"))
                .distrito("Miraflores")
                .ciudad("Lima")
                .build();
        ZonaRiesgo zonaRiesgo = ZonaRiesgo.builder()
                .idZona(5)
                .tipo("ROBO")
                .nivelRiesgo(3)
                .descripcion("Zona con alta incidencia reportada")
                .estado(Boolean.TRUE)
                .coordenadasGeojson(
                        "{\"type\":\"Polygon\",\"coordinates\":[[[-77.0310,-12.1215],[-77.0300,-12.1215],[-77.0300,-12.1207],[-77.0310,-12.1207],[-77.0310,-12.1215]]]}"
                )
                .fechaActualizacion(LocalDateTime.of(2026, 5, 12, 10, 0))
                .ubicacion(ubicacion)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));
        when(ubicacionRepository.findByCiudadIgnoreCaseAndDistritoIgnoreCase("Lima", "Miraflores"))
                .thenReturn(List.of(ubicacion));
        when(zonaRiesgoRepository.findByEstadoTrueAndUbicacion_IdUbicacionInOrderByNivelRiesgoDescFechaActualizacionDesc(
                List.of(10)))
                .thenReturn(List.of(zonaRiesgo));

        RiskZoneMapResponseDTO response = riskZoneMapService.getActiveRiskZonesForMap(
                "luis.rojas@demo.com",
                RiskZoneMapRequestDTO.builder()
                        .ciudad("Lima")
                        .distrito("Miraflores")
                        .build());

        assertEquals(1, response.getTotalZonas());
        assertEquals("Se encontraron zonas de riesgo activas.", response.getMensaje());
        assertEquals("alto", response.getZonas().get(0).getNivelRiesgoNombre());
        assertEquals("#DC2626", response.getZonas().get(0).getColor());
        assertEquals("Miraflores", response.getZonas().get(0).getCentro().getDistrito());
    }

    @Test
    void getActiveRiskZonesForMapShouldReturnEmptyResponseWhenCityHasNoZones() {
        Usuario user = buildActiveUser();

        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));
        when(ubicacionRepository.findByCiudadIgnoreCase("Arequipa")).thenReturn(List.of());

        RiskZoneMapResponseDTO response = riskZoneMapService.getActiveRiskZonesForMap(
                "luis.rojas@demo.com",
                RiskZoneMapRequestDTO.builder()
                        .ciudad("Arequipa")
                        .build());

        assertEquals(0, response.getTotalZonas());
        assertEquals("No existen zonas de riesgo registradas para la ubicacion seleccionada.", response.getMensaje());
        assertEquals(0, response.getZonas().size());
    }

    @Test
    void getActiveRiskZonesForMapShouldThrowWhenCityIsMissing() {
        Usuario user = buildActiveUser();
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(user));

        InvalidRiskZoneRequestException exception = assertThrows(
                InvalidRiskZoneRequestException.class,
                () -> riskZoneMapService.getActiveRiskZonesForMap(
                        "luis.rojas@demo.com",
                        RiskZoneMapRequestDTO.builder().ciudad(" ").build()));

        assertEquals("La ciudad es obligatoria", exception.getMessage());
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
