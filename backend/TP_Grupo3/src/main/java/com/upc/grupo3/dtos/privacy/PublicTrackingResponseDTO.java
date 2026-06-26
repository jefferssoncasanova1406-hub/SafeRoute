package com.upc.grupo3.dtos.privacy;

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
public class PublicTrackingResponseDTO {
    private String nombreUsuario;
    private Double latitudActual;
    private Double longitudActual;
    private String ultimaActualizacion;
    private String estadoRuta; // "EN_CAMINO", "FINALIZADA"
}
