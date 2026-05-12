package com.upc.av_2.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
public class IncidenteDTO {

    private Integer idIncidente;

    @NotBlank(message = "El tipo de incidente es obligatorio")
    @Size(max = 100, message = "El tipo de incidente no puede superar los 100 caracteres")
    private String tipoIncidente;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "La fecha del incidente es obligatoria")
    @PastOrPresent(message = "La fecha del incidente no puede ser futura")
    private LocalDate fechaIncidente;

    @NotBlank(message = "La fuente es obligatoria")
    @Size(max = 100, message = "La fuente no puede superar los 100 caracteres")
    private String fuente;

    @NotNull(message = "La ubicacion es obligatoria")
    private Integer ubicacionIdUbicacio;
}
