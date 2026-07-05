package com.upc.grupo3.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upc.grupo3.dtos.privacy.AlertHistoryRequestDTO;
import com.upc.grupo3.dtos.privacy.AlertHistoryResponseDTO;
import com.upc.grupo3.dtos.privacy.CommunityVoteRequestDTO;
import com.upc.grupo3.dtos.privacy.ModerationRequestDTO;
import com.upc.grupo3.entidades.EstadoLecturaAlerta;
import com.upc.grupo3.entidades.EstadoModeracionIncidente;
import com.upc.grupo3.entidades.IncidenteCiudadano;
import com.upc.grupo3.entidades.OrigenIncidente;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.exceptions.DuplicateCommunityVoteException;
import com.upc.grupo3.exceptions.IncidentAlreadyModeratedException;
import com.upc.grupo3.exceptions.InvalidCommunityAlertRequestException;
import com.upc.grupo3.exceptions.ResourceNotFoundException;
import com.upc.grupo3.repositories.IncidenteCiudadanoRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.repositories.VerificacionComunitariaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AlertCommunityServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private IncidenteCiudadanoRepository incidenteCiudadanoRepository;

    @Mock
    private VerificacionComunitariaRepository verificacionComunitariaRepository;

    @InjectMocks
    private AlertCommunityService service;

    @Test
    void historyShouldQueryRepositoryOrderedByFechaEmisionDesc() {
        Usuario usuario = user(1, "luis.rojas@demo.com");
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(systemIncident(101), systemIncident(102)));

        List<AlertHistoryResponseDTO> response = service.getAlertsHistory(
                "luis.rojas@demo.com", null, null, null, null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(incidenteCiudadanoRepository).findAll(any(Specification.class), sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().getOrderFor("fechaEmision");
        assertNotNull(order);
        assertTrue(order.isDescending());
        assertEquals(List.of(101, 102), response.stream().map(AlertHistoryResponseDTO::getIdAlerta).toList());
    }

    @Test
    void historyShouldAcceptTipoEstadoAndInclusiveDateFilters() {
        Usuario usuario = user(1, "luis.rojas@demo.com");
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(systemIncident(101)));

        List<AlertHistoryResponseDTO> response = service.getAlertsHistory(
                "luis.rojas@demo.com", "robo", "no_leida", "2026-05-12", "2026-05-12");

        verify(incidenteCiudadanoRepository).findAll(any(Specification.class), any(Sort.class));
        assertEquals(1, response.size());
        assertEquals("NO_LEIDA", response.get(0).getEstado());
    }

    @Test
    void historyShouldRejectInvalidDateFormat() {
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com"))
                .thenReturn(Optional.of(user(1, "luis.rojas@demo.com")));

        InvalidCommunityAlertRequestException exception = assertThrows(
                InvalidCommunityAlertRequestException.class,
                () -> service.getAlertsHistory("luis.rojas@demo.com", null, null, "12/05/2026", null));

        assertEquals("El formato de fecha debe ser yyyy-MM-dd.", exception.getMessage());
    }

    @Test
    void historyShouldRejectStartDateAfterEndDate() {
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com"))
                .thenReturn(Optional.of(user(1, "luis.rojas@demo.com")));

        InvalidCommunityAlertRequestException exception = assertThrows(
                InvalidCommunityAlertRequestException.class,
                () -> service.getAlertsHistory("luis.rojas@demo.com", null, null, "2026-05-13", "2026-05-12"));

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de fin.", exception.getMessage());
    }

    @Test
    void detailShouldReturnPersistedDemoAlerts101102And103() {
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com"))
                .thenReturn(Optional.of(user(1, "luis.rojas@demo.com")));
        when(incidenteCiudadanoRepository.findById(101)).thenReturn(Optional.of(systemIncident(101)));
        when(incidenteCiudadanoRepository.findById(102)).thenReturn(Optional.of(systemIncident(102)));
        when(incidenteCiudadanoRepository.findById(103)).thenReturn(Optional.of(systemIncident(103)));

        assertEquals(101, service.getAlertDetail("luis.rojas@demo.com", 101).getIdAlerta());
        assertEquals(102, service.getAlertDetail("luis.rojas@demo.com", 102).getIdAlerta());
        assertEquals(103, service.getAlertDetail("luis.rojas@demo.com", 103).getIdAlerta());
    }

    @Test
    void detailShouldReturn404WhenIncidentDoesNotExist() {
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com"))
                .thenReturn(Optional.of(user(1, "luis.rojas@demo.com")));
        when(incidenteCiudadanoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAlertDetail("luis.rojas@demo.com", 999));
    }

    @Test
    void registerIncidentShouldPersistTrimmedPendingReportWithGeneratedId() {
        Usuario usuario = user(2, "luis.rojas@demo.com");
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.save(any(IncidenteCiudadano.class))).thenAnswer(invocation -> {
            IncidenteCiudadano incidente = invocation.getArgument(0);
            incidente.setIdAlerta(700);
            return incidente;
        });

        AlertHistoryResponseDTO response = service.registerIncidentReport(
                "luis.rojas@demo.com",
                AlertHistoryRequestDTO.builder()
                        .tipoIncidente(" Robo ")
                        .ubicacion(" San Miguel ")
                        .descripcion(" Descripcion valida ")
                        .build());

        assertEquals(700, response.getIdAlerta());
        assertEquals("Robo", response.getTipoIncidente());
        assertEquals("San Miguel", response.getZonaAfectada());
        assertEquals("Descripcion valida", response.getDescripcion());
        assertEquals("PENDIENTE", response.getEstado());
    }

    @Test
    void registeredIncidentShouldAppearInHistoryModerationAndDetailWhenRepositoryReturnsIt() {
        Usuario usuario = user(2, "luis.rojas@demo.com");
        IncidenteCiudadano incidente = citizenIncident(700, EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(incidente));
        when(incidenteCiudadanoRepository.findAllByEstadoModeracionOrderByFechaEmisionDesc(EstadoModeracionIncidente.PENDIENTE))
                .thenReturn(List.of(incidente));
        when(incidenteCiudadanoRepository.findById(700)).thenReturn(Optional.of(incidente));

        assertEquals(700, service.getAlertsHistory("luis.rojas@demo.com", null, null, null, null).get(0).getIdAlerta());
        assertEquals(700, service.getPendingReportsForModeration("luis.rojas@demo.com").get(0).getIdAlerta());
        assertEquals(700, service.getAlertDetail("luis.rojas@demo.com", 700).getIdAlerta());
    }

    @Test
    void firstConfirmedVoteShouldPersistAndPreserveIncidentData() {
        Usuario usuario = user(2, "luis.rojas@demo.com");
        IncidenteCiudadano incidente = citizenIncident(700, EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findById(700)).thenReturn(Optional.of(incidente));
        when(verificacionComunitariaRepository.existsByIncidenteAndUsuario(incidente, usuario)).thenReturn(false);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.TRUE)).thenReturn(1L);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.FALSE)).thenReturn(0L);
        when(incidenteCiudadanoRepository.save(incidente)).thenReturn(incidente);

        AlertHistoryResponseDTO response = service.verifyCommunityIncident(
                "luis.rojas@demo.com",
                CommunityVoteRequestDTO.builder().idIncidente(700).verificado(true).build());

        verify(verificacionComunitariaRepository).saveAndFlush(any());
        assertEquals("Robo", response.getTipoIncidente());
        assertEquals("Reporte ciudadano persistido", response.getDescripcion());
        assertEquals("San Miguel", response.getZonaAfectada());
        assertEquals("ALTO (CONFIRMADO POR COMUNIDAD)", response.getNivelRiesgo());
    }

    @Test
    void firstRejectedVoteShouldPersistAndLowerRisk() {
        Usuario usuario = user(2, "luis.rojas@demo.com");
        IncidenteCiudadano incidente = citizenIncident(700, EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findById(700)).thenReturn(Optional.of(incidente));
        when(verificacionComunitariaRepository.existsByIncidenteAndUsuario(incidente, usuario)).thenReturn(false);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.TRUE)).thenReturn(0L);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.FALSE)).thenReturn(1L);
        when(incidenteCiudadanoRepository.save(incidente)).thenReturn(incidente);

        AlertHistoryResponseDTO response = service.verifyCommunityIncident(
                "luis.rojas@demo.com",
                CommunityVoteRequestDTO.builder().idIncidente(700).verificado(false).build());

        assertEquals("BAJO (RECHAZADO POR COMUNIDAD)", response.getNivelRiesgo());
    }

    @Test
    void duplicateVoteShouldReturnConflictException() {
        Usuario usuario = user(2, "luis.rojas@demo.com");
        IncidenteCiudadano incidente = citizenIncident(700, EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(usuario));
        when(incidenteCiudadanoRepository.findById(700)).thenReturn(Optional.of(incidente));
        when(verificacionComunitariaRepository.existsByIncidenteAndUsuario(incidente, usuario)).thenReturn(true);

        DuplicateCommunityVoteException exception = assertThrows(
                DuplicateCommunityVoteException.class,
                () -> service.verifyCommunityIncident(
                        "luis.rojas@demo.com",
                        CommunityVoteRequestDTO.builder().idIncidente(700).verificado(true).build()));

        assertEquals(
                "Ya registraste una verificación para ese reporte. No se permiten votos duplicados.",
                exception.getMessage());
    }

    @Test
    void anotherUserCanVoteSameIncident() {
        Usuario firstUser = user(2, "luis.rojas@demo.com");
        Usuario secondUser = user(4, "otro@demo.com");
        IncidenteCiudadano incidente = citizenIncident(700, EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase("luis.rojas@demo.com")).thenReturn(Optional.of(firstUser));
        when(usuarioRepository.findByEmailIgnoreCase("otro@demo.com")).thenReturn(Optional.of(secondUser));
        when(incidenteCiudadanoRepository.findById(700)).thenReturn(Optional.of(incidente));
        when(verificacionComunitariaRepository.existsByIncidenteAndUsuario(any(), any())).thenReturn(false);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.TRUE)).thenReturn(1L, 2L);
        when(verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.FALSE)).thenReturn(0L);
        when(incidenteCiudadanoRepository.save(incidente)).thenReturn(incidente);

        service.verifyCommunityIncident(
                "luis.rojas@demo.com",
                CommunityVoteRequestDTO.builder().idIncidente(700).verificado(true).build());
        AlertHistoryResponseDTO response = service.verifyCommunityIncident(
                "otro@demo.com",
                CommunityVoteRequestDTO.builder().idIncidente(700).verificado(true).build());

        assertEquals(700, response.getIdAlerta());
    }

    @Test
    void pendingModerationShouldListOnlyPendingCitizenReports() {
        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com"))
                .thenReturn(Optional.of(user(1, "ana.torres@demo.com")));
        when(incidenteCiudadanoRepository.findAllByEstadoModeracionOrderByFechaEmisionDesc(EstadoModeracionIncidente.PENDIENTE))
                .thenReturn(List.of(
                        citizenIncident(700, EstadoModeracionIncidente.PENDIENTE),
                        systemIncident(101)));

        List<AlertHistoryResponseDTO> response = service.getPendingReportsForModeration("ana.torres@demo.com");

        assertEquals(1, response.size());
        assertEquals(700, response.get(0).getIdAlerta());
        assertEquals("PENDIENTE", response.get(0).getEstado());
    }

    @Test
    void moderationShouldApproveRejectAndMarkFalseWithSpecificMessages() {
        assertModeration(EstadoModeracionIncidente.APROBADO, "Reporte aprobado con éxito.");
        assertModeration(EstadoModeracionIncidente.RECHAZADO, "Reporte rechazado con éxito.");
        assertModeration(EstadoModeracionIncidente.FALSO, "Reporte marcado como falso con éxito.");
    }

    @Test
    void moderationShouldRejectInvalidStatus() {
        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com"))
                .thenReturn(Optional.of(user(1, "ana.torres@demo.com")));

        assertThrows(
                InvalidCommunityAlertRequestException.class,
                () -> service.moderateIncident(
                        "ana.torres@demo.com",
                        ModerationRequestDTO.builder().idIncidente(700).nuevoEstado("PENDIENTE").build()));
    }

    @Test
    void moderationShouldRejectAlreadyProcessedReport() {
        when(usuarioRepository.findByEmailIgnoreCase("ana.torres@demo.com"))
                .thenReturn(Optional.of(user(1, "ana.torres@demo.com")));
        when(incidenteCiudadanoRepository.findById(700))
                .thenReturn(Optional.of(citizenIncident(700, EstadoModeracionIncidente.APROBADO)));

        assertThrows(
                IncidentAlreadyModeratedException.class,
                () -> service.moderateIncident(
                        "ana.torres@demo.com",
                        ModerationRequestDTO.builder().idIncidente(700).nuevoEstado("RECHAZADO").build()));
    }

    private void assertModeration(EstadoModeracionIncidente targetStatus, String expectedMessage) {
        String email = "ana.torres@demo.com";
        IncidenteCiudadano incidente = citizenIncident(700 + targetStatus.ordinal(), EstadoModeracionIncidente.PENDIENTE);
        when(usuarioRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user(1, email)));
        when(incidenteCiudadanoRepository.findById(incidente.getIdAlerta())).thenReturn(Optional.of(incidente));
        when(incidenteCiudadanoRepository.save(incidente)).thenReturn(incidente);

        AlertHistoryResponseDTO response = service.moderateIncident(
                email,
                ModerationRequestDTO.builder()
                        .idIncidente(incidente.getIdAlerta())
                        .nuevoEstado(targetStatus.name())
                        .build());

        assertEquals(targetStatus.name(), response.getEstado());
        assertEquals(expectedMessage, response.getMessage());
        assertEquals("Robo", response.getTipoIncidente());
        assertEquals("Reporte ciudadano persistido", response.getDescripcion());
    }

    private Usuario user(Integer id, String email) {
        return Usuario.builder()
                .idUsuario(id)
                .email(email)
                .nombre("Usuario Demo")
                .estado(Boolean.TRUE)
                .fechaRegistro(LocalDate.of(2026, 1, 1))
                .build();
    }

    private IncidenteCiudadano systemIncident(Integer id) {
        return IncidenteCiudadano.builder()
                .idAlerta(id)
                .tipoIncidente(id == 102 ? "Accidente" : id == 103 ? "Asalto" : "Robo")
                .descripcion("Descripcion persistida " + id)
                .nivelRiesgo(id == 102 ? "MEDIO" : "ALTO")
                .fechaEmision(LocalDateTime.of(2026, 5, 12, 9, 0).minusHours(id - 101L))
                .estadoLectura(id == 101 ? EstadoLecturaAlerta.NO_LEIDA : EstadoLecturaAlerta.LEIDA)
                .estadoModeracion(EstadoModeracionIncidente.APROBADO)
                .origen(OrigenIncidente.SISTEMA)
                .zonaAfectada(id == 102 ? "San Miguel" : id == 103 ? "Cercado de Lima" : "Santiago de Surco")
                .build();
    }

    private IncidenteCiudadano citizenIncident(Integer id, EstadoModeracionIncidente status) {
        return IncidenteCiudadano.builder()
                .idAlerta(id)
                .tipoIncidente("Robo")
                .descripcion("Reporte ciudadano persistido")
                .nivelRiesgo("MEDIO")
                .fechaEmision(LocalDateTime.of(2026, 5, 13, 10, 0))
                .estadoLectura(EstadoLecturaAlerta.NO_LEIDA)
                .estadoModeracion(status)
                .origen(OrigenIncidente.CIUDADANO)
                .zonaAfectada("San Miguel")
                .reportante(user(2, "luis.rojas@demo.com"))
                .build();
    }
}
