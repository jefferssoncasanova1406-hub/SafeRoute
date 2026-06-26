package com.upc.grupo3.dtos.privacy;

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
public class LanguageConfigResponseDTO {
    private String currentLanguageCode; // Idioma activo final
    private String statusMessage;
    private Boolean isPreterminado;     // Indica si se aplicó el idioma por defecto
    private Map<String, String> translationSample; // Pequeña muestra de textos clave traducidos para control del frontend
}
