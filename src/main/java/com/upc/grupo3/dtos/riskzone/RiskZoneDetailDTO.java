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
public class RiskZoneDetailDTO {

    private Integer idZona;
    private String tipo;
    private Integer nivelRiesgo;
    private String descripcion;
    private String estado;
    private LocalDateTime fechaActualizacion;
    private RiskZoneGeometryDTO geometria;
    private RiskZoneLocationDTO ubicacion;
}
