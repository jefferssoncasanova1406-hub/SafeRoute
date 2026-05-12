package com.upc.av_2.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Incidente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidente")
    private Integer idIncidente;

    @Column(name = "tipo_incidente", nullable = false, length = 100)
    private String tipoIncidente;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "fecha_incidente", nullable = false)
    private LocalDate fechaIncidente;

    @Column(name = "fuente", nullable = false, length = 100)
    private String fuente;

    @Column(name = "Ubicacion_ID_Ubicacio", nullable = false)
    private Integer ubicacionIdUbicacio;
}
