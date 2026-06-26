package com.upc.grupo3.dtos.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class LanguageChangeRequestDTO {
    @NotBlank(message = "El código de idioma es obligatorio (ej. 'es', 'en')")
    @Size(min = 2, max = 5, message = "El código de idioma debe tener entre 2 y 5 caracteres")
    private String languageCode;
}
