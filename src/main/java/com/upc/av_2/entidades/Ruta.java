package com.upc.av_2.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Ruta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer idRuta;

    @Column(name = "nivel_seguridad", nullable = false)
    private Integer nivelSeguridad;

    @Column(name = "distancia", nullable = false)
    private Integer distancia;

    @Column(name = "tiempo_estimado", nullable = false)
    private Integer tiempoEstimado;

    @Column(name = "Usuario_ID_Usuari", nullable = false)
    private Integer usuarioIdUsuari;
}
