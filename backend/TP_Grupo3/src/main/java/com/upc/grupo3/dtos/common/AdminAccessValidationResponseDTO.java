package com.upc.grupo3.dtos.common;

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
public class AdminAccessValidationResponseDTO {

    private Boolean authorized;
    private String role;
    private String resource;
    private String action;
    private String message;
}
