package com.upc.grupo3.dtos.saferoute;

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

    private SafeRoutePointDTO origen;
    private SafeRoutePointDTO destino;
    private SafeRouteOptionDTO rutaMasRapida;
    private SafeRouteOptionDTO rutaMasSegura;
    private SafeRouteOptionDTO rutaRecomendada;
    private String nivelRiesgo;
    private Integer scoreRiesgo;
    private Integer tiempoEstimado;
    private Integer distancia;
    private String recomendacion;
}
