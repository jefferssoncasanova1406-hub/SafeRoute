package com.upc.grupo3.dtos.privacy;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityOperationResponseDTO {
    private String ciudadKey;       // Ej: "LIMA", "AREQUIPA"
    private String nombreFormateado; // Ej: "Lima Metropolitana"
    private Double centroLatitud;
    private Double centroLongitud;
    private Boolean soportada;
    private String message;
}
