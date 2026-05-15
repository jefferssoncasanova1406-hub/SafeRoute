package com.upc.av_2.services;

import com.upc.av_2.dtos.RiskZoneDetailDTO;
import com.upc.av_2.dtos.RiskZoneGeometryDTO;
import com.upc.av_2.dtos.RiskZoneListResponseDTO;
import com.upc.av_2.dtos.RiskZoneLocationDTO;
import com.upc.av_2.dtos.RiskZoneOperationResponseDTO;
import com.upc.av_2.dtos.RiskZoneRequestDTO;
import com.upc.av_2.entidades.Ubicacion;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.entidades.ZonaRiesgo;
import com.upc.av_2.exceptions.AccountDisabledException;
import com.upc.av_2.exceptions.ApplicationConfigurationException;
import com.upc.av_2.exceptions.InvalidRiskZoneRequestException;
import com.upc.av_2.exceptions.ResourceNotFoundException;
import com.upc.av_2.exceptions.UnauthenticatedUserException;
import com.upc.av_2.repositories.UbicacionRepository;
import com.upc.av_2.repositories.UsuarioRepository;
import com.upc.av_2.repositories.ZonaRiesgoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskZoneService {

    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_INACTIVA = "INACTIVA";
    private static final String GEOMETRY_TYPE_POLYGON = "Polygon";

    private final ZonaRiesgoRepository zonaRiesgoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeometryJsonService geometryJsonService;
    private final RiskLevelCatalog riskLevelCatalog;

    @Transactional
    public RiskZoneOperationResponseDTO createRiskZone(String authenticatedEmail, RiskZoneRequestDTO request) {
        Usuario administrador = resolveActiveAuthenticatedUser(authenticatedEmail);
        validateRiskZoneRequest(request);

        Ubicacion ubicacion = ubicacionRepository.save(buildUbicacion(request.getUbicacion()));
        LocalDateTime now = LocalDateTime.now();

        ZonaRiesgo zonaRiesgo = ZonaRiesgo.builder()
                .tipo(normalizeText(request.getTipo()).toUpperCase(Locale.ROOT))
                .nivelRiesgo(request.getNivelRiesgo())
                .descripcion(normalizeText(request.getDescripcion()))
                .estado(Boolean.TRUE)
                .coordenadasGeojson(serializeGeometry(normalizeGeometry(request.getGeometria())))
                .fechaActualizacion(now)
                .ubicacion(ubicacion)
                .build();

        ZonaRiesgo zonaCreada = zonaRiesgoRepository.save(zonaRiesgo);
        log.info("Zona de riesgo creada zoneId={} adminId={} ubicacionId={} nivelRiesgo={}",
                zonaCreada.getIdZona(),
                administrador.getIdUsuario(),
                ubicacion.getIdUbicacion(),
                zonaCreada.getNivelRiesgo());

        return RiskZoneOperationResponseDTO.builder()
                .message("Zona de riesgo creada correctamente")
                .zona(buildRiskZoneDetail(zonaCreada, ubicacion))
                .build();
    }

    @Transactional
    public RiskZoneOperationResponseDTO updateRiskZone(
            String authenticatedEmail,
            Integer idZona,
            RiskZoneRequestDTO request) {
        Usuario administrador = resolveActiveAuthenticatedUser(authenticatedEmail);
        validateRiskZoneId(idZona);
        validateRiskZoneRequest(request);

        ZonaRiesgo zonaRiesgo = findRiskZoneById(idZona);
        Ubicacion ubicacion = getRequiredUbicacion(zonaRiesgo, idZona);

        zonaRiesgo.setTipo(normalizeText(request.getTipo()).toUpperCase(Locale.ROOT));
        zonaRiesgo.setNivelRiesgo(request.getNivelRiesgo());
        zonaRiesgo.setDescripcion(normalizeText(request.getDescripcion()));
        zonaRiesgo.setCoordenadasGeojson(serializeGeometry(normalizeGeometry(request.getGeometria())));
        zonaRiesgo.setFechaActualizacion(LocalDateTime.now());

        updateUbicacion(ubicacion, request.getUbicacion());

        Ubicacion ubicacionActualizada = ubicacionRepository.save(ubicacion);
        ZonaRiesgo zonaActualizada = zonaRiesgoRepository.save(zonaRiesgo);
        log.info("Zona de riesgo actualizada zoneId={} adminId={} ubicacionId={} nivelRiesgo={} estado={}",
                zonaActualizada.getIdZona(),
                administrador.getIdUsuario(),
                ubicacionActualizada.getIdUbicacion(),
                zonaActualizada.getNivelRiesgo(),
                zonaActualizada.getEstado());

        return RiskZoneOperationResponseDTO.builder()
                .message("Zona de riesgo actualizada correctamente")
                .zona(buildRiskZoneDetail(zonaActualizada, ubicacionActualizada))
                .build();
    }

    @Transactional
    public RiskZoneOperationResponseDTO deactivateRiskZone(String authenticatedEmail, Integer idZona) {
        Usuario administrador = resolveActiveAuthenticatedUser(authenticatedEmail);
        validateRiskZoneId(idZona);

        ZonaRiesgo zonaRiesgo = findRiskZoneById(idZona);
        if (!Boolean.TRUE.equals(zonaRiesgo.getEstado())) {
            log.warn("Desactivacion rechazada porque la zona ya esta inactiva zoneId={}", idZona);
            throw new InvalidRiskZoneRequestException("La zona de riesgo ya se encuentra inactiva");
        }

        zonaRiesgo.setEstado(Boolean.FALSE);
        zonaRiesgo.setFechaActualizacion(LocalDateTime.now());

        ZonaRiesgo zonaActualizada = zonaRiesgoRepository.save(zonaRiesgo);
        Ubicacion ubicacion = getRequiredUbicacion(zonaActualizada, idZona);
        log.info("Zona de riesgo desactivada zoneId={} adminId={} ubicacionId={}",
                zonaActualizada.getIdZona(),
                administrador.getIdUsuario(),
                ubicacion.getIdUbicacion());

        return RiskZoneOperationResponseDTO.builder()
                .message("Zona de riesgo desactivada correctamente")
                .zona(buildRiskZoneDetail(zonaActualizada, ubicacion))
                .build();
    }

    @Transactional(readOnly = true)
    public RiskZoneListResponseDTO getRiskZones(
            String authenticatedEmail,
            String estado,
            Integer nivelRiesgo) {
        resolveActiveAuthenticatedUser(authenticatedEmail);

        Boolean estadoFiltro = parseEstado(estado);
        validateRiskLevelFilter(nivelRiesgo);

        List<ZonaRiesgo> zonas = findRiskZonesByFilters(estadoFiltro, nivelRiesgo);
        Map<Integer, Ubicacion> ubicaciones = loadUbicaciones(zonas);
        List<RiskZoneDetailDTO> zonasDetalle = zonas.stream()
                .map(zona -> buildRiskZoneDetail(zona, ubicaciones.get(zona.getUbicacion().getIdUbicacion())))
                .collect(Collectors.toList());

        log.info("Consulta de zonas de riesgo completada total={} estadoFiltro={} nivelRiesgo={}",
                zonasDetalle.size(), estadoFiltro, nivelRiesgo);

        return RiskZoneListResponseDTO.builder()
                .message("Zonas de riesgo obtenidas correctamente")
                .zonas(zonasDetalle)
                .build();
    }

    private Usuario resolveActiveAuthenticatedUser(String authenticatedEmail) {
        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Operacion de zonas de riesgo rechazada por falta de sesion autenticada");
            throw new UnauthenticatedUserException("No existe una sesion autenticada para gestionar zonas de riesgo");
        }

        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Usuario autenticado no encontrado durante gestion de zonas de riesgo email={}",
                            normalizedEmail);
                    return new ResourceNotFoundException("No se encontro el usuario autenticado");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Operacion de zonas de riesgo rechazada por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        return usuario;
    }

    private void validateRiskZoneId(Integer idZona) {
        if (idZona == null || idZona <= 0) {
            log.warn("Solicitud de zona de riesgo rechazada por identificador invalido zoneId={}", idZona);
            throw new InvalidRiskZoneRequestException(
                    "El identificador de la zona de riesgo es obligatorio y debe ser mayor que cero");
        }
    }

    private void validateRiskZoneRequest(RiskZoneRequestDTO request) {
        if (request == null) {
            log.warn("Solicitud de zona de riesgo rechazada porque el cuerpo es nulo");
            throw new InvalidRiskZoneRequestException("La solicitud de zona de riesgo es obligatoria");
        }

        validateGeometry(request.getGeometria());
    }

    private void validateRiskLevelFilter(Integer nivelRiesgo) {
        if (nivelRiesgo != null && !riskLevelCatalog.isSupported(nivelRiesgo)) {
            log.warn("Consulta de zonas de riesgo rechazada por nivelRiesgo invalido nivelRiesgo={}", nivelRiesgo);
            throw new InvalidRiskZoneRequestException("El nivel de riesgo debe estar entre 1 y 3");
        }
    }

    private void validateGeometry(RiskZoneGeometryDTO geometria) {
        if (geometria == null) {
            throw new InvalidRiskZoneRequestException("La geometria es obligatoria");
        }

        if (!GEOMETRY_TYPE_POLYGON.equalsIgnoreCase(normalizeText(geometria.getType()))) {
            throw new InvalidRiskZoneRequestException("La geometria debe ser de tipo Polygon");
        }

        List<List<List<BigDecimal>>> coordinates = geometria.getCoordinates();
        if (coordinates == null || coordinates.isEmpty()) {
            throw new InvalidRiskZoneRequestException("La geometria debe contener al menos un anillo de coordenadas");
        }

        for (List<List<BigDecimal>> ring : coordinates) {
            validateRing(ring);
        }
    }

    private void validateRing(List<List<BigDecimal>> ring) {
        if (ring == null || ring.size() < 4) {
            throw new InvalidRiskZoneRequestException(
                    "Cada anillo del poligono debe tener al menos cuatro puntos");
        }

        for (List<BigDecimal> point : ring) {
            validatePoint(point);
        }

        List<BigDecimal> firstPoint = ring.get(0);
        List<BigDecimal> lastPoint = ring.get(ring.size() - 1);
        if (firstPoint.get(0).compareTo(lastPoint.get(0)) != 0
                || firstPoint.get(1).compareTo(lastPoint.get(1)) != 0) {
            throw new InvalidRiskZoneRequestException(
                    "El primer y el ultimo punto del poligono deben ser iguales");
        }
    }

    private void validatePoint(List<BigDecimal> point) {
        if (point == null || point.size() != 2) {
            throw new InvalidRiskZoneRequestException(
                    "Cada punto de la geometria debe contener longitud y latitud");
        }

        BigDecimal longitud = point.get(0);
        BigDecimal latitud = point.get(1);
        if (longitud == null || latitud == null) {
            throw new InvalidRiskZoneRequestException(
                    "Cada punto de la geometria debe contener longitud y latitud");
        }

        if (longitud.compareTo(new BigDecimal("-180")) < 0 || longitud.compareTo(new BigDecimal("180")) > 0) {
            throw new InvalidRiskZoneRequestException(
                    "La longitud de cada punto de la geometria debe estar entre -180 y 180");
        }

        if (latitud.compareTo(new BigDecimal("-90")) < 0 || latitud.compareTo(new BigDecimal("90")) > 0) {
            throw new InvalidRiskZoneRequestException(
                    "La latitud de cada punto de la geometria debe estar entre -90 y 90");
        }
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : null;
    }

    private RiskZoneGeometryDTO normalizeGeometry(RiskZoneGeometryDTO geometria) {
        return RiskZoneGeometryDTO.builder()
                .type(GEOMETRY_TYPE_POLYGON)
                .coordinates(geometria.getCoordinates())
                .build();
    }

    private Ubicacion buildUbicacion(RiskZoneLocationDTO ubicacion) {
        return Ubicacion.builder()
                .latitud(ubicacion.getLatitud())
                .longitud(ubicacion.getLongitud())
                .distrito(normalizeText(ubicacion.getDistrito()))
                .ciudad(normalizeText(ubicacion.getCiudad()))
                .build();
    }

    private void updateUbicacion(Ubicacion target, RiskZoneLocationDTO source) {
        target.setLatitud(source.getLatitud());
        target.setLongitud(source.getLongitud());
        target.setDistrito(normalizeText(source.getDistrito()));
        target.setCiudad(normalizeText(source.getCiudad()));
    }

    private ZonaRiesgo findRiskZoneById(Integer idZona) {
        return zonaRiesgoRepository.findById(idZona)
                .orElseThrow(() -> {
                    log.warn("Zona de riesgo no encontrada zoneId={}", idZona);
                    return new ResourceNotFoundException("No se encontro la zona de riesgo");
                });
    }

    private Ubicacion getRequiredUbicacion(ZonaRiesgo zonaRiesgo, Integer idZona) {
        Ubicacion ubicacion = zonaRiesgo.getUbicacion();
        if (ubicacion == null) {
            log.error("Inconsistencia de datos: ubicacion no encontrada zoneId={}", idZona);
            throw new ApplicationConfigurationException(
                    "No se encontro la ubicacion asociada a la zona de riesgo con id " + idZona);
        }
        return ubicacion;
    }

    private List<ZonaRiesgo> findRiskZonesByFilters(Boolean estado, Integer nivelRiesgo) {
        if (estado != null && nivelRiesgo != null) {
            return zonaRiesgoRepository.findByEstadoAndNivelRiesgo(estado, nivelRiesgo);
        }
        if (estado != null) {
            return zonaRiesgoRepository.findByEstado(estado);
        }
        if (nivelRiesgo != null) {
            return zonaRiesgoRepository.findByNivelRiesgo(nivelRiesgo);
        }
        return zonaRiesgoRepository.findAll();
    }

    private Map<Integer, Ubicacion> loadUbicaciones(List<ZonaRiesgo> zonas) {
        List<Integer> ubicacionIds = zonas.stream()
                .map(zona -> {
                    Ubicacion ubicacion = zona.getUbicacion();
                    if (ubicacion == null) {
                        log.error("Inconsistencia de datos: falta una ubicacion asociada zoneId={}", zona.getIdZona());
                        throw new ApplicationConfigurationException(
                                "No se encontro la ubicacion asociada a una zona de riesgo");
                    }
                    return ubicacion.getIdUbicacion();
                })
                .distinct()
                .toList();

        Map<Integer, Ubicacion> ubicaciones = new HashMap<>();
        for (Ubicacion ubicacion : ubicacionRepository.findAllById(ubicacionIds)) {
            ubicaciones.put(ubicacion.getIdUbicacion(), ubicacion);
        }

        for (Integer ubicacionId : ubicacionIds) {
            if (!ubicaciones.containsKey(ubicacionId)) {
                log.error("Inconsistencia de datos: falta una ubicacion asociada ubicacionId={}", ubicacionId);
                throw new ApplicationConfigurationException(
                        "No se encontro la ubicacion asociada a una zona de riesgo");
            }
        }
        return ubicaciones;
    }

    private Boolean parseEstado(String estado) {
        if (!StringUtils.hasText(estado)) {
            return null;
        }

        String normalizedEstado = estado.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedEstado) {
            case "ACTIVA", "ACTIVE", "TRUE" -> Boolean.TRUE;
            case "INACTIVA", "INACTIVE", "FALSE" -> Boolean.FALSE;
            default -> {
                log.warn("Consulta de zonas de riesgo rechazada por estado invalido estado={}", estado);
                throw new InvalidRiskZoneRequestException("El estado debe ser ACTIVA o INACTIVA");
            }
        };
    }

    private String serializeGeometry(RiskZoneGeometryDTO geometria) {
        return geometryJsonService.write(
                geometria,
                "No se pudo serializar la geometria de la zona de riesgo",
                "No se pudo serializar la geometria de la zona de riesgo");
    }

    private RiskZoneGeometryDTO deserializeGeometry(String geometriaJson) {
        return geometryJsonService.read(
                geometriaJson,
                RiskZoneGeometryDTO.class,
                "No se pudo deserializar la geometria almacenada de la zona de riesgo",
                "No se pudo deserializar la geometria de la zona de riesgo");
    }

    private RiskZoneDetailDTO buildRiskZoneDetail(ZonaRiesgo zonaRiesgo, Ubicacion ubicacion) {
        return RiskZoneDetailDTO.builder()
                .idZona(zonaRiesgo.getIdZona())
                .tipo(zonaRiesgo.getTipo())
                .nivelRiesgo(zonaRiesgo.getNivelRiesgo())
                .descripcion(zonaRiesgo.getDescripcion())
                .estado(Boolean.TRUE.equals(zonaRiesgo.getEstado()) ? ESTADO_ACTIVA : ESTADO_INACTIVA)
                .fechaActualizacion(zonaRiesgo.getFechaActualizacion())
                .geometria(deserializeGeometry(zonaRiesgo.getCoordenadasGeojson()))
                .ubicacion(RiskZoneLocationDTO.builder()
                        .latitud(ubicacion.getLatitud())
                        .longitud(ubicacion.getLongitud())
                        .distrito(ubicacion.getDistrito())
                        .ciudad(ubicacion.getCiudad())
                        .build())
                .build();
    }
}
