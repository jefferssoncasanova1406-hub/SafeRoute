package com.upc.grupo3.dtos.privacy;

import java.time.LocalDateTime;
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
public class ShareLinkResponseDTO {
    private String tokenSeguimiento;
    private String urlCompleta; // Ej: "https://saferoute.pe/shared/tracking/abc123xyz"
    private LocalDateTime fechaExpiracionEstimada;
    private String estadoLink; // "ACTIVO", "EXPIRADO"
}
