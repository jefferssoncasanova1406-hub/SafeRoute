package com.upc.av_2.dtos;

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
public class SafeRouteRiskZoneDTO {

    private Integer idZona;
    private String tipo;
    private Integer nivelRiesgo;
    private String nivelRiesgoNombre;
    private String color;
    private String descripcion;
    private RiskZoneLocationDTO centro;
}
