package com.upc.grupo3.dtos.auth;

import jakarta.validation.constraints.NotBlank;
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
public class SuspendUserRequestDTO {

    @NotNull(message = "El ID del usuario a suspender es obligatorio")
    private Integer idUsuario;

    @NotBlank(message = "El motivo de la suspensión es obligatorio para la auditoría")
    private String motivo;
}
