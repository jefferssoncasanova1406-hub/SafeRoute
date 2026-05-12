package com.upc.av_2.dtos;

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
public class AdminAccessValidationRequestDTO {

    @NotBlank(message = "El recurso es obligatorio")
    @Size(max = 100, message = "El recurso no puede exceder los 100 caracteres")
    private String resource;

    @NotBlank(message = "La accion es obligatoria")
    @Size(max = 20, message = "La accion no puede exceder los 20 caracteres")
    private String action;
}
