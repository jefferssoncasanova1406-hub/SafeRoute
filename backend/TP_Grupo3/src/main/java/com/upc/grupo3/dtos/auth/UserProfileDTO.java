package com.upc.grupo3.dtos.auth;

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
public class UserProfileDTO {

    private String nombre;
    private String email;
    private String preferenciasRiesg;
    private BigDecimal radioAlerta;
    private Boolean notificacionesActi;
}
