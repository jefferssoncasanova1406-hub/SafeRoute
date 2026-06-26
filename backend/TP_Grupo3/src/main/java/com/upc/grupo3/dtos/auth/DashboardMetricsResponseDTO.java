package com.upc.grupo3.dtos.auth;

import java.util.List;
import java.util.Map;
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
public class DashboardMetricsResponseDTO {
    // Escenario 1: Indicadores generales
    private Long totalIncidentes;
    private Integer zonasActivasCount;
    private String nivelRiesgoPredominante; // "ALTO", "MEDIO", "BAJO"

    // Escenario 2: Agrupación por criterios (Mapeos clave-valor dinámicos)
    private Map<String, Long> incidentesPorZona;   // Ej: {"Surco": 45, "Chorrillos": 30}
    private Map<String, Long> incidentesPorTipo;   // Ej: {"Robo": 60, "Asalto": 15}
    private Map<String, Long> incidentesPorPeriodo; // Ej: {"Mayo": 40, "Junio": 50}

    // Escenario 3: Datos para la matriz del mapa de calor geográfico
    private List<HeatMapPointDTO> puntosMapaCalor;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatMapPointDTO {
        private Double latitud;
        private Double longitud;
        private Double intensidad; // Nivel de concentración del riesgo de 0.0 a 1.0
    }
}
