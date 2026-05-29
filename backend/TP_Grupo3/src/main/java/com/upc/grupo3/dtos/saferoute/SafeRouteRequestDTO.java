package com.upc.grupo3.dtos.saferoute;

import jakarta.validation.Valid;
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
public class SafeRouteRequestDTO {

    @NotNull(message = "El origen es obligatorio")
    @Valid
    private SafeRoutePointDTO origen;

    @NotNull(message = "El destino es obligatorio")
    @Valid
    private SafeRoutePointDTO destino;
}
