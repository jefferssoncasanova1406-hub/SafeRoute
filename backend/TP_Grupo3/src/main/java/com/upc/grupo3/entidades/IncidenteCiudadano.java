package com.upc.grupo3.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "incidente_ciudadano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidenteCiudadano extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @Column(name = "tipo_incidente", nullable = false, length = 80)
    private String tipoIncidente;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "nivel_riesgo", nullable = false, length = 80)
    private String nivelRiesgo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_lectura", nullable = false, length = 20)
    private EstadoLecturaAlerta estadoLectura;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion", nullable = false, length = 20)
    private EstadoModeracionIncidente estadoModeracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private OrigenIncidente origen;

    @Column(name = "zona_afectada", nullable = false, length = 150)
    private String zonaAfectada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportante_id_usuario")
    private Usuario reportante;
}
