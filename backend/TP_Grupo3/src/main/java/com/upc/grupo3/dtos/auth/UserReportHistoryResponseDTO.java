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
public class UserReportHistoryResponseDTO {
    private Integer idUsuario;
    private String nombreUsuario;
    private String emailUsuario;
    private Integer cantidadReportesFalsos;
    private String estadoActual; // "ACTIVO", "SUSPENDIDO"
    private List<String> historialReportesFalsos; // Lista de descripciones o IDs de reportes marcados como falsos
}
