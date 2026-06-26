package com.upc.grupo3.dtos.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ModerationRequestDTO {

    @NotNull(message = "El ID del incidente a moderar es obligatorio")
    private Integer idIncidente;

    @NotBlank(message = "El estado final es obligatorio (APROBADO / RECHAZADO / FALSO)")
    private String nuevoEstado;
}
