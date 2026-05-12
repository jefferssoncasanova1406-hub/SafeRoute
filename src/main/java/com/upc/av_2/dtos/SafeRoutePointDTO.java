package com.upc.av_2.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class SafeRoutePointDTO {

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "La latitud debe tener hasta 7 decimales")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "La longitud debe tener hasta 7 decimales")
    private BigDecimal longitud;

    @Size(max = 150, message = "La referencia no puede superar los 150 caracteres")
    private String referencia;

    @Size(max = 100, message = "El distrito no puede superar los 100 caracteres")
    private String distrito;

    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;
}
