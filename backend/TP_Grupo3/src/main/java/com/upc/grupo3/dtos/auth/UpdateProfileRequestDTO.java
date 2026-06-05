package com.upc.grupo3.dtos.auth;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateProfileRequestDTO {

    @NotBlank(message = "El nombre es obligatorio y no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "La preferencia de riesgo es obligatoria")
    @Size(max = 50, message = "La preferencia de riesgo no puede superar los 50 caracteres")
    private String preferenciasRiesg;

    @NotNull(message = "El radio de alerta no puede ser nulo")
    private BigDecimal radioAlerta;

    @NotNull(message = "El estado de las notificaciones es obligatorio")
    private Boolean notificacionesActi;
}
