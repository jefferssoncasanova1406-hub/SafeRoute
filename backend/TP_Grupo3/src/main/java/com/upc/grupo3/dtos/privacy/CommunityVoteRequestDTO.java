package com.upc.grupo3.dtos.privacy;

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
public class CommunityVoteRequestDTO {

    @NotNull(message = "El ID del incidente a verificar es obligatorio")
    private Integer idIncidente;

    @NotNull(message = "Debe especificar si confirma (true) o rechaza (false) el incidente")
    private Boolean verificado; // true = Confirmar, false = Rechazar
}
