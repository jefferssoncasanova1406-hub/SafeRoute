package com.upc.av_2.entidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EntidadAuditableTest {

    @Test
    void registrarCreacionShouldInitializeAuditoriaWhenFieldIsMissing() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1)
                .nombre("Ana Torres")
                .email("ana.torres@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 10))
                .estado(Boolean.TRUE)
                .rolIdRol(1)
                .build();
        usuario.setAuditoria(null);

        usuario.registrarCreacion();

        assertNotNull(usuario.getAuditoria());
        assertNotNull(usuario.getAuditoria().getFechaCreacion());
        assertNotNull(usuario.getAuditoria().getFechaModificacion());
        assertEquals(
                usuario.getAuditoria().getFechaCreacion(),
                usuario.getAuditoria().getFechaModificacion());
    }

    @Test
    void registrarModificacionShouldPreserveCreationDateAndRefreshModificationDate() {
        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis Rojas")
                .email("luis.rojas@demo.com")
                .contrasena("$2a$10$hash")
                .fechaRegistro(LocalDate.of(2026, 1, 15))
                .estado(Boolean.TRUE)
                .rolIdRol(2)
                .build();
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 5, 1, 9, 0);
        LocalDateTime fechaModificacion = LocalDateTime.of(2026, 5, 1, 9, 30);
        usuario.setAuditoria(Auditoria.builder()
                .fechaCreacion(fechaCreacion)
                .fechaModificacion(fechaModificacion)
                .build());

        usuario.registrarModificacion();

        assertEquals(fechaCreacion, usuario.getAuditoria().getFechaCreacion());
        assertTrue(usuario.getAuditoria().getFechaModificacion().isAfter(fechaModificacion));
    }
}
