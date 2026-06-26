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
public class GlobalAlertRequestDTO {

    @NotBlank(message = "El título de la alerta global es obligatorio")
    private String titulo;

    @NotBlank(message = "El mensaje o cuerpo de la emergencia es obligatorio")
    private String mensaje;

    @NotBlank(message = "Debe especificar el nivel de prioridad (ALTA / CRÍTICA)")
    private String nivelPrioridad;

    @NotBlank(message = "El alcance geográfico es obligatorio (ej. Toda la ciudad, Sector Sur)")
    private String alcance;
}
