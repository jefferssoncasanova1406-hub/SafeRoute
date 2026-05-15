package com.upc.grupo3.dtos.common;

import com.upc.grupo3.dtos.auth.AuthenticatedUserDTO;

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
public class ProtectedResourceResponseDTO {

    private String message;
    private AuthenticatedUserDTO user;
}
