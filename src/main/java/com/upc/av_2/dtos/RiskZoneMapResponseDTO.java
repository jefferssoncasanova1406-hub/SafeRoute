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
public class RiskZoneMapResponseDTO {

    private RiskZoneMapLocationDTO ubicacion;
    private Integer totalZonas;
    private String mensaje;
    private List<RiskZoneMapZoneDTO> zonas;
}
