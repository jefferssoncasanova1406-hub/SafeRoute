package com.upc.grupo3.dtos.saferoute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
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
public class SafeRouteGeometryDTO {

    @NotBlank(message = "El tipo de geometria es obligatorio")
    private String type;

    @NotNull(message = "Las coordenadas de la geometria son obligatorias")
    @NotEmpty(message = "Las coordenadas de la geometria no pueden estar vacias")
    private List<List<BigDecimal>> coordinates;
}
