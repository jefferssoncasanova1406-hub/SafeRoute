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
public class AlertHistoryRequestDTO {

    @NotBlank(message = "El tipo de incidente es obligatorio (ej. Robo, Asalto, Accidente)")
    private String tipoIncidente;

    @NotBlank(message = "La ubicación aproximada es obligatoria para mapear el riesgo")
    private String ubicacion;

    @NotBlank(message = "La descripción del incidente es obligatoria")
    @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
    private String descripcion;
}