package com.upc.grupo3.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
public class Perfil extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Perfil")
    private Integer idPerfil;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_Usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "Preferencias_Riesg", nullable = false, length = 50)
    private String preferenciasRiesg;

    @Column(name = "Radio_Alerta", nullable = false, precision = 10, scale = 7)
    private BigDecimal radioAlerta;

    @Column(name = "Notificaciones_Acti", nullable = false)
    private Boolean notificacionesActi;
}
