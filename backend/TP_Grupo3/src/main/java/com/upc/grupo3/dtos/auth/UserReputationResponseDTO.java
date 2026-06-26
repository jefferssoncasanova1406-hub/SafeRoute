package com.upc.grupo3.dtos.auth;

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
public class UserReputationResponseDTO {
    private Integer idUsuario;
    private String nombre;
    private Integer reportesVerificadosCount; // Escenario 1
    private Integer reportesFalsosExcluidosCount; // Escenario 3
    private String rangoActual; // Hito alcanzado (ej. "Colaborador de Oro")
    private List<String> recompensasObtenidas; // Escenario 2: Lista de medallas o reconocimientos
}
