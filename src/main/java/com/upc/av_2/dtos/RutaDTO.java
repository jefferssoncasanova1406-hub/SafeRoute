package com.upc.av_2.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class RutaDTO {

    private Integer idRuta;

    @NotNull(message = "El nivel de seguridad es obligatorio")
    @Positive(message = "El nivel de seguridad debe ser mayor que cero")
    private Integer nivelSeguridad;

    @NotNull(message = "La distancia es obligatoria")
    @Positive(message = "La distancia debe ser mayor que cero")
    private Integer distancia;

    @NotNull(message = "El tiempo estimado es obligatorio")
    @Positive(message = "El tiempo estimado debe ser mayor que cero")
    private Integer tiempoEstimado;

    @NotNull(message = "El usuario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser mayor que cero")
    private Integer usuarioIdUsuari;
}
