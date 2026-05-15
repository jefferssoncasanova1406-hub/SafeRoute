package com.upc.grupo3.dtos.auth;

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
public class RegisteredUserDTO {

    private Integer id;
    private String nombre;
    private String email;
    private String rol;
}
