package com.upc.av_2.entidades;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class EntidadAuditable {

    @Embedded
    private Auditoria auditoria = new Auditoria();

    @PrePersist
    void registrarCreacion() {
        if (auditoria == null) {
            auditoria = new Auditoria();
        }
        auditoria.registrarCreacion();
    }

    @PreUpdate
    void registrarModificacion() {
        if (auditoria == null) {
            auditoria = new Auditoria();
        }
        auditoria.registrarModificacion();
    }
}
