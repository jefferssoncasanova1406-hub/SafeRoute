package com.upc.grupo3.dtos.routeevaluation;

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
@Builder(toBuilder = true)
public class RouteOptionDTO {

    private String routeId;
    private String summary;
    private Double durationMinutes;
    private Double distanceKm;
    private GeoJsonLineStringDTO geometry;
    private List<RouteStepDTO> steps;
    private String nivelRiesgo;
    private Integer scoreRiesgo;
    private Boolean cruzaZonasRiesgo;
    private List<RouteRiskZoneDTO> zonasRiesgo;
    private Boolean recomendada;
}