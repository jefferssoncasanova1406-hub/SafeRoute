package com.upc.grupo3.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.grupo3.dtos.riskzone.RiskZoneGeometryDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneLocationDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapLocationDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapRequestDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapResponseDTO;
import com.upc.grupo3.dtos.riskzone.RiskZoneMapZoneDTO;
import com.upc.grupo3.entidades.Ubicacion;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.entidades.ZonaRiesgo;
import com.upc.grupo3.exceptions.AccountDisabledException;
import com.upc.grupo3.exceptions.ApplicationConfigurationException;
import com.upc.grupo3.exceptions.InvalidRiskZoneRequestException;
import com.upc.grupo3.exceptions.ResourceNotFoundException;
import com.upc.grupo3.exceptions.UnauthenticatedUserException;
import com.upc.grupo3.repositories.UbicacionRepository;
import com.upc.grupo3.repositories.UsuarioRepository;
import com.upc.grupo3.repositories.ZonaRiesgoRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskZoneMapService {

    private static final int LOW_RISK_LEVEL = 1;
    private static final int MEDIUM_RISK_LEVEL = 2;
    private static final int HIGH_RISK_LEVEL = 3;

    private final ZonaRiesgoRepository zonaRiesgoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RiskZoneMapResponseDTO getActiveRiskZonesForMap(
            String authenticatedEmail,
            RiskZoneMapRequestDTO request) {
        resolveActiveAuthenticatedUser(authenticatedEmail);
        RiskZoneMapRequestDTO normalizedRequest = normalizeAndValidateRequest(request);

        List<Ubicacion> ubicaciones = findLocations(normalizedRequest);
        if (ubicaciones.isEmpty()) {
            return buildEmptyResponse(normalizedRequest);
        }

        Map<Integer, Ubicacion> ubicacionesById = new HashMap<>();
        List<Integer> ubicacionIds = ubicaciones.stream()
                .map(Ubicacion::getIdUbicacion)
                .toList();
        for (Ubicacion ubicacion : ubicaciones) {
            ubicacionesById.put(ubicacion.getIdUbicacion(), ubicacion);
        }

        List<ZonaRiesgo> zonas = zonaRiesgoRepository
                .findByEstadoTrueAndUbicacion_IdUbicacionInOrderByNivelRiesgoDescFechaActualizacionDesc(ubicacionIds);
        if (zonas.isEmpty()) {
            return buildEmptyResponse(normalizedRequest);
        }

        List<RiskZoneMapZoneDTO> zonasResponse = zonas.stream()
                .map(zona -> buildMapZone(zona, ubicacionesById.get(zona.getUbicacion().getIdUbicacion())))
                .toList();

        log.info("Consulta de mapa completada email={} ciudad={} distrito={} totalZonas={}",
                authenticatedEmail,
                normalizedRequest.getCiudad(),
                normalizedRequest.getDistrito(),
                zonasResponse.size());

        return RiskZoneMapResponseDTO.builder()
                .ubicacion(RiskZoneMapLocationDTO.builder()
                        .ciudad(normalizedRequest.getCiudad())
                        .distrito(normalizedRequest.getDistrito())
                        .build())
                .totalZonas(zonasResponse.size())
                .mensaje("Se encontraron zonas de riesgo activas.")
                .zonas(zonasResponse)
                .build();
    }

    private Usuario resolveActiveAuthenticatedUser(String authenticatedEmail) {
        if (!StringUtils.hasText(authenticatedEmail)) {
            log.warn("Consulta de mapa rechazada por falta de sesion autenticada");
            throw new UnauthenticatedUserException("No existe una sesion autenticada para consultar el mapa");
        }

        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Usuario autenticado no encontrado durante consulta de mapa email={}", normalizedEmail);
                    return new ResourceNotFoundException("No se encontro el usuario autenticado");
                });

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Consulta de mapa rechazada por cuenta no habilitada userId={} email={}",
                    usuario.getIdUsuario(), usuario.getEmail());
            throw new AccountDisabledException("La cuenta no se encuentra habilitada");
        }

        return usuario;
    }

    private RiskZoneMapRequestDTO normalizeAndValidateRequest(RiskZoneMapRequestDTO request) {
        if (request == null) {
            log.warn("Consulta de mapa rechazada porque la solicitud es nula");
            throw new InvalidRiskZoneRequestException("La solicitud del mapa es obligatoria");
        }

        String ciudad = normalizeText(request.getCiudad());
        String distrito = normalizeText(request.getDistrito());

        if (!StringUtils.hasText(ciudad)) {
            log.warn("Consulta de mapa rechazada porque la ciudad es obligatoria");
            throw new InvalidRiskZoneRequestException("La ciudad es obligatoria");
        }
        if (ciudad.length() > 100) {
            log.warn("Consulta de mapa rechazada porque la ciudad supera el tamano permitido ciudad={}", ciudad);
            throw new InvalidRiskZoneRequestException("La ciudad no puede superar los 100 caracteres");
        }
        if (distrito != null && distrito.length() > 100) {
            log.warn("Consulta de mapa rechazada porque el distrito supera el tamano permitido distrito={}", distrito);
            throw new InvalidRiskZoneRequestException("El distrito no puede superar los 100 caracteres");
        }

        return RiskZoneMapRequestDTO.builder()
                .ciudad(ciudad)
                .distrito(StringUtils.hasText(distrito) ? distrito : null)
                .build();
    }

    private List<Ubicacion> findLocations(RiskZoneMapRequestDTO request) {
        if (StringUtils.hasText(request.getDistrito())) {
            return ubicacionRepository.findByCiudadIgnoreCaseAndDistritoIgnoreCase(
                    request.getCiudad(),
                    request.getDistrito());
        }
        return ubicacionRepository.findByCiudadIgnoreCase(request.getCiudad());
    }

    private RiskZoneMapResponseDTO buildEmptyResponse(RiskZoneMapRequestDTO request) {
        log.info("Consulta de mapa sin zonas activas ciudad={} distrito={}",
                request.getCiudad(), request.getDistrito());
        return RiskZoneMapResponseDTO.builder()
                .ubicacion(RiskZoneMapLocationDTO.builder()
                        .ciudad(request.getCiudad())
                        .distrito(request.getDistrito())
                        .build())
                .totalZonas(0)
                .mensaje("No existen zonas de riesgo registradas para la ubicacion seleccionada.")
                .zonas(List.of())
                .build();
    }

    private RiskZoneMapZoneDTO buildMapZone(ZonaRiesgo zonaRiesgo, Ubicacion ubicacion) {
        if (ubicacion == null) {
            log.error("Inconsistencia de datos: ubicacion faltante para zoneId={}", zonaRiesgo.getIdZona());
            throw new ApplicationConfigurationException(
                    "No se encontro la ubicacion asociada a una zona de riesgo activa");
        }

        return RiskZoneMapZoneDTO.builder()
                .idZona(zonaRiesgo.getIdZona())
                .tipo(zonaRiesgo.getTipo())
                .nivelRiesgo(zonaRiesgo.getNivelRiesgo())
                .nivelRiesgoNombre(resolveRiskLevelName(zonaRiesgo.getNivelRiesgo()))
                .color(resolveRiskLevelColor(zonaRiesgo.getNivelRiesgo()))
                .descripcion(zonaRiesgo.getDescripcion())
                .fechaActualizacion(zonaRiesgo.getFechaActualizacion())
                .geometria(deserializeGeometry(zonaRiesgo.getCoordenadasGeojson()))
                .centro(RiskZoneLocationDTO.builder()
                        .latitud(ubicacion.getLatitud())
                        .longitud(ubicacion.getLongitud())
                        .distrito(ubicacion.getDistrito())
                        .ciudad(ubicacion.getCiudad())
                        .build())
                .build();
    }

    private String resolveRiskLevelName(Integer nivelRiesgo) {
        return switch (nivelRiesgo) {
            case LOW_RISK_LEVEL -> "bajo";
            case MEDIUM_RISK_LEVEL -> "medio";
            case HIGH_RISK_LEVEL -> "alto";
            default -> throw new ApplicationConfigurationException("La zona de riesgo tiene un nivel no soportado");
        };
    }

    private String resolveRiskLevelColor(Integer nivelRiesgo) {
        return switch (nivelRiesgo) {
            case LOW_RISK_LEVEL -> "#22C55E";
            case MEDIUM_RISK_LEVEL -> "#F59E0B";
            case HIGH_RISK_LEVEL -> "#DC2626";
            default -> throw new ApplicationConfigurationException("La zona de riesgo tiene un nivel no soportado");
        };
    }

    private RiskZoneGeometryDTO deserializeGeometry(String geometriaJson) {
        try {
            return objectMapper.readValue(geometriaJson, RiskZoneGeometryDTO.class);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo deserializar la geometria almacenada de la zona de riesgo", exception);
            throw new ApplicationConfigurationException(
                    "No se pudo deserializar la geometria de la zona de riesgo");
        }
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : null;
    }
}
