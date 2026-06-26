package com.upc.grupo3.dtos.auth;

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
public class AuditLogResponseDTO {
    private Long idAuditoria;
    private String administrador;   // Correo del admin responsable
    private String accion;          // Ej: "SUSPENSION_CUENTA", "BROADCAST_ALERTA"
    private LocalDateTime fechaHora; // Fecha y hora exacta del evento
    private String entidadAfectada; // Ej: "Usuario ID: 452", "GlobalAlert ID: 8812"
    private String detalles;        // Motivo o información adicional del cambio
}
