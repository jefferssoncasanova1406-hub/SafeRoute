package com.upc.grupo3.dtos.privacy;

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
public class AlertHistoryResponseDTO {
    private Integer idAlerta;
    private String tipoIncidente; // Ej: "Robo", "Asalto", "Accidente"
    private String descripcion;
    private String nivelRiesgo; // "BAJO", "MEDIO", "ALTO"
    private LocalDateTime fechaEmision;
    private String estado; // "LEIDA", "NO_LEIDA"
    private String zonaAfectada; // Nombre del distrito o zona
    private String message;
}
