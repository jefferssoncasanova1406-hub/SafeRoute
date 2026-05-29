package com.upc.grupo3.dtos.riskzone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class RiskZoneRequestDTO {

    @NotBlank(message = "El tipo de zona de riesgo es obligatorio")
    @Size(max = 50, message = "El tipo no puede superar los 50 caracteres")
    private String tipo;

    @NotNull(message = "El nivel de riesgo es obligatorio")
    @Min(value = 1, message = "El nivel de riesgo debe estar entre 1 y 3")
    @Max(value = 3, message = "El nivel de riesgo debe estar entre 1 y 3")
    private Integer nivelRiesgo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    private String descripcion;

    @Valid
    @NotNull(message = "La geometria es obligatoria")
    private RiskZoneGeometryDTO geometria;

    @Valid
    @NotNull(message = "La ubicacion es obligatoria")
    private RiskZoneLocationDTO ubicacion;
}
