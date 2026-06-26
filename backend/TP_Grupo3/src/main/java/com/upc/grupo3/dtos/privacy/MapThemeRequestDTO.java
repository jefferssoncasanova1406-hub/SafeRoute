package com.upc.grupo3.dtos.privacy;

import jakarta.validation.constraints.NotBlank;
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
public class MapThemeRequestDTO {
    @NotBlank(message = "El identificador del tema es obligatorio (ej. 'DARK', 'LIGHT', 'SATELLITE')")
    private String themeKey;
}
