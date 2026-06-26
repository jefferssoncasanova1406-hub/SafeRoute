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
public class MapThemeResponseDTO {
    private String activeThemeKey;
    private String jsonStyleUrl; // Ruta al archivo JSON de estilos de mapa (ej. Google Maps JSON / Mapbox Style)
    private Boolean persistido;
    private String message;
}
