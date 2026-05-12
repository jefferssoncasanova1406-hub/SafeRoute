package com.upc.av_2.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Perfil")
    private Integer idPerfil;

    @Column(name = "ID_Usuario", nullable = false, unique = true)
    private Integer idUsuario;

    @Column(name = "Preferencias_Riesg", nullable = false, length = 50)
    private String preferenciasRiesg;

    @Column(name = "Radio_Alerta", nullable = false, precision = 10, scale = 7)
    private BigDecimal radioAlerta;

    @Column(name = "Notificaciones_Acti", nullable = false)
    private Boolean notificacionesActi;
}
