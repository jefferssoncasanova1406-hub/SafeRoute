package com.upc.grupo3.dtos.saferoute;

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
public class SafeRouteOptionDTO {

    private Integer distancia;
    private Integer tiempoEstimado;
    private Integer scoreRiesgo;
    private String nivelRiesgo;
    private Boolean cruzaZonasRiesgo;
    private SafeRouteGeometryDTO geometria;
    private List<SafeRouteRiskZoneDTO> zonasRiesgo;
}
