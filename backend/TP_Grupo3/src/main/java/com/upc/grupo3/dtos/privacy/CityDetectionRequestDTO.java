package com.upc.grupo3.dtos.privacy;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityDetectionRequestDTO {
    @NotNull(message = "La latitud es obligatoria para la autodetección")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria para la autodetección")
    private Double longitud;
}
