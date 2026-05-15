package com.upc.grupo3.dtos.riskzone;

import java.time.LocalDateTime;
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
public class RiskZoneMapZoneDTO {

    private Integer idZona;
    private String tipo;
    private Integer nivelRiesgo;
    private String nivelRiesgoNombre;
    private String color;
    private String descripcion;
    private LocalDateTime fechaActualizacion;
    private RiskZoneGeometryDTO geometria;
    private RiskZoneLocationDTO centro;
}
