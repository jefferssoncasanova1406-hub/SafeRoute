package com.upc.av_2.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeRouteResponseDTO {

    private Integer rutaId;
    private String mensaje;
    private SafeRoutePointDTO origen;
    private SafeRoutePointDTO destino;
    private Integer distanciaMetros;
    private Integer tiempoEstimadoMinutos;
    private Integer nivelRiesgo;
    private String nivelRiesgoNombre;
    private Boolean cruzaZonasRiesgo;
    private List<String> recomendaciones;
    private SafeRouteGeometryDTO geometria;
    private List<SafeRouteRiskZoneDTO> zonasRiesgo;
}
