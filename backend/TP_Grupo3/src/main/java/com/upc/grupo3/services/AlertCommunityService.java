package com.upc.grupo3.services;

import com.upc.grupo3.dtos.privacy.AlertHistoryRequestDTO;
import com.upc.grupo3.dtos.privacy.AlertHistoryResponseDTO;
import com.upc.grupo3.dtos.privacy.CommunityVoteRequestDTO;
import com.upc.grupo3.dtos.privacy.ModerationRequestDTO;
import com.upc.grupo3.entidades.EstadoLecturaAlerta;
import com.upc.grupo3.entidades.EstadoModeracionIncidente;
import com.upc.grupo3.entidades.IncidenteCiudadano;
import com.upc.grupo3.entidades.OrigenIncidente;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.entidades.VerificacionComunitaria;
import com.upc.grupo3.exceptions.DuplicateCommunityVoteException;
import com.upc.grupo3.exceptions.IncidentAlreadyModeratedException;
import com.upc.grupo3.exceptions.InvalidCommunityAlertRequestException;
import com.upc.grupo3.exceptions.ResourceNotFoundException;
import com.upc.grupo3.repositories.IncidenteCiudadanoRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.repositories.VerificacionComunitariaRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertCommunityService {

    private static final String DUPLICATE_VOTE_MESSAGE =
            "Ya registraste una verificación para ese reporte. No se permiten votos duplicados.";

    private final UsuarioRepository usuarioRepository;
    private final IncidenteCiudadanoRepository incidenteCiudadanoRepository;
    private final VerificacionComunitariaRepository verificacionComunitariaRepository;

    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getAlertsHistory(
            String email, String tipoIncidente, String estado, String fechaInicio, String fechaFin) {
        findAuthenticatedUser(email);
        DateRange dateRange = parseDateRange(fechaInicio, fechaFin);

        Specification<IncidenteCiudadano> specification = buildHistorySpecification(
                tipoIncidente, estado, dateRange);

        return incidenteCiudadanoRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "fechaEmision"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertHistoryResponseDTO getAlertDetail(String email, Integer idAlerta) {
        findAuthenticatedUser(email);
        IncidenteCiudadano incidente = findIncident(idAlerta);
        return toResponse(incidente);
    }

    @Transactional
    public AlertHistoryResponseDTO registerIncidentReport(String email, AlertHistoryRequestDTO request) {
        Usuario usuario = findAuthenticatedUser(email);

        IncidenteCiudadano incidente = IncidenteCiudadano.builder()
                .tipoIncidente(trimRequired(request.getTipoIncidente(), "El tipo de incidente es obligatorio"))
                .zonaAfectada(trimRequired(request.getUbicacion(), "La ubicación aproximada es obligatoria"))
                .descripcion(trimRequired(request.getDescripcion(), "La descripción del incidente es obligatoria"))
                .nivelRiesgo("MEDIO")
                .fechaEmision(LocalDateTime.now())
                .estadoLectura(EstadoLecturaAlerta.NO_LEIDA)
                .estadoModeracion(EstadoModeracionIncidente.PENDIENTE)
                .origen(OrigenIncidente.CIUDADANO)
                .reportante(usuario)
                .build();

        IncidenteCiudadano saved = incidenteCiudadanoRepository.save(incidente);
        return toResponse(saved, "Reporte de incidente enviado correctamente. Queda pendiente de revisión administrativa.");
    }

    @Transactional
    public AlertHistoryResponseDTO verifyCommunityIncident(String email, CommunityVoteRequestDTO request) {
        Usuario usuario = findAuthenticatedUser(email);
        IncidenteCiudadano incidente = findIncident(request.getIdIncidente());

        if (verificacionComunitariaRepository.existsByIncidenteAndUsuario(incidente, usuario)) {
            throw new DuplicateCommunityVoteException(DUPLICATE_VOTE_MESSAGE);
        }

        VerificacionComunitaria verificacion = VerificacionComunitaria.builder()
                .incidente(incidente)
                .usuario(usuario)
                .verificado(request.getVerificado())
                .fechaVotacion(LocalDateTime.now())
                .build();

        try {
            verificacionComunitariaRepository.saveAndFlush(verificacion);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCommunityVoteException(DUPLICATE_VOTE_MESSAGE);
        }

        updateRiskLevelFromVotes(incidente);
        IncidenteCiudadano saved = incidenteCiudadanoRepository.save(incidente);

        String message = Boolean.TRUE.equals(request.getVerificado())
                ? "Voto registrado. El nivel de confianza del reporte ha aumentado con éxito."
                : "Voto registrado. El nivel de confianza del reporte ha sido recalculado a la baja.";
        return toResponse(saved, message);
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getPendingReportsForModeration(String email) {
        findAuthenticatedUser(email);
        return incidenteCiudadanoRepository
                .findAllByEstadoModeracionOrderByFechaEmisionDesc(EstadoModeracionIncidente.PENDIENTE)
                .stream()
                .filter(incidente -> incidente.getOrigen() == OrigenIncidente.CIUDADANO)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AlertHistoryResponseDTO moderateIncident(String email, ModerationRequestDTO request) {
        findAuthenticatedUser(email);
        EstadoModeracionIncidente nuevoEstado = parseModerationStatus(request.getNuevoEstado());
        IncidenteCiudadano incidente = findIncident(request.getIdIncidente());

        if (incidente.getEstadoModeracion() != EstadoModeracionIncidente.PENDIENTE) {
            throw new IncidentAlreadyModeratedException("El reporte ya fue moderado y no puede procesarse nuevamente.");
        }

        incidente.setEstadoModeracion(nuevoEstado);
        IncidenteCiudadano saved = incidenteCiudadanoRepository.save(incidente);
        return toResponse(saved, moderationMessage(nuevoEstado));
    }

    private Specification<IncidenteCiudadano> buildHistorySpecification(
            String tipoIncidente, String estado, DateRange dateRange) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(tipoIncidente)) {
                predicates.add(cb.equal(
                        cb.lower(root.get("tipoIncidente")),
                        tipoIncidente.trim().toLowerCase(Locale.ROOT)));
            }
            if (dateRange.start() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), dateRange.start()));
            }
            if (dateRange.end() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), dateRange.end()));
            }
            if (StringUtils.hasText(estado)) {
                Predicate estadoPredicate = buildEstadoPredicate(estado, root, cb);
                predicates.add(estadoPredicate);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate buildEstadoPredicate(
            String estado,
            jakarta.persistence.criteria.Root<IncidenteCiudadano> root,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        String normalized = estado.trim().toUpperCase(Locale.ROOT);
        List<Predicate> predicates = new ArrayList<>();

        Arrays.stream(EstadoLecturaAlerta.values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .ifPresent(value -> predicates.add(cb.and(
                        cb.equal(root.get("origen"), OrigenIncidente.SISTEMA),
                        cb.equal(root.get("estadoLectura"), value))));

        Arrays.stream(EstadoModeracionIncidente.values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .ifPresent(value -> predicates.add(cb.and(
                        cb.equal(root.get("origen"), OrigenIncidente.CIUDADANO),
                        cb.equal(root.get("estadoModeracion"), value))));

        if (predicates.isEmpty()) {
            return cb.disjunction();
        }
        return cb.or(predicates.toArray(Predicate[]::new));
    }

    private DateRange parseDateRange(String fechaInicio, String fechaFin) {
        LocalDateTime start = parseDate(fechaInicio, true);
        LocalDateTime end = parseDate(fechaFin, false);

        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidCommunityAlertRequestException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        return new DateRange(start, end);
    }

    private LocalDateTime parseDate(String value, boolean startOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException exception) {
            throw new InvalidCommunityAlertRequestException("El formato de fecha debe ser yyyy-MM-dd.");
        }
    }

    private EstadoModeracionIncidente parseModerationStatus(String value) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidCommunityAlertRequestException("El estado de moderación es obligatorio.");
        }
        try {
            EstadoModeracionIncidente estado = EstadoModeracionIncidente.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (estado == EstadoModeracionIncidente.PENDIENTE) {
                throw new InvalidCommunityAlertRequestException(
                        "Estado de moderación inválido. Valores permitidos: APROBADO, RECHAZADO, FALSO.");
            }
            return estado;
        } catch (IllegalArgumentException exception) {
            throw new InvalidCommunityAlertRequestException(
                    "Estado de moderación inválido. Valores permitidos: APROBADO, RECHAZADO, FALSO.");
        }
    }

    private void updateRiskLevelFromVotes(IncidenteCiudadano incidente) {
        long confirmed = verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.TRUE);
        long rejected = verificacionComunitariaRepository.countByIncidenteAndVerificado(incidente, Boolean.FALSE);

        if (confirmed > rejected) {
            incidente.setNivelRiesgo("ALTO (CONFIRMADO POR COMUNIDAD)");
        } else if (rejected > confirmed) {
            incidente.setNivelRiesgo("BAJO (RECHAZADO POR COMUNIDAD)");
        } else {
            incidente.setNivelRiesgo("MEDIO (EN VERIFICACION)");
        }
    }

    private Usuario findAuthenticatedUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResourceNotFoundException("Usuario autenticado no encontrado");
        }
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private IncidenteCiudadano findIncident(Integer idAlerta) {
        return incidenteCiudadanoRepository.findById(idAlerta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la alerta solicitada con el id: " + idAlerta));
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidCommunityAlertRequestException(message);
        }
        return value.trim();
    }

    private AlertHistoryResponseDTO toResponse(IncidenteCiudadano incidente) {
        return toResponse(incidente, null);
    }

    private AlertHistoryResponseDTO toResponse(IncidenteCiudadano incidente, String message) {
        return AlertHistoryResponseDTO.builder()
                .idAlerta(incidente.getIdAlerta())
                .tipoIncidente(incidente.getTipoIncidente())
                .descripcion(incidente.getDescripcion())
                .nivelRiesgo(incidente.getNivelRiesgo())
                .fechaEmision(incidente.getFechaEmision())
                .estado(publicStatus(incidente))
                .zonaAfectada(incidente.getZonaAfectada())
                .message(message)
                .build();
    }

    private String publicStatus(IncidenteCiudadano incidente) {
        if (incidente.getOrigen() == OrigenIncidente.CIUDADANO) {
            return incidente.getEstadoModeracion().name();
        }
        return incidente.getEstadoLectura().name();
    }

    private String moderationMessage(EstadoModeracionIncidente estado) {
        return switch (estado) {
            case APROBADO -> "Reporte aprobado con éxito.";
            case RECHAZADO -> "Reporte rechazado con éxito.";
            case FALSO -> "Reporte marcado como falso con éxito.";
            case PENDIENTE -> throw new InvalidCommunityAlertRequestException(
                    "Estado de moderación inválido. Valores permitidos: APROBADO, RECHAZADO, FALSO.");
        };
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
