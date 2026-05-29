package com.upc.grupo3.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Column(name = "auditoria_fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "auditoria_fecha_modificacion")
    private LocalDateTime fechaModificacion;

    public void registrarCreacion() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaModificacion = ahora;
    }

    public void registrarModificacion() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaModificacion = ahora;
    }
}
