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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Configuracion_Privacidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionPrivacidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Configuracion_Privacidad")
    private Integer idConfiguracionPrivacidad;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_Usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "Ubicacion_Tiempo_Real", nullable = false)
    private Boolean ubicacionTiempoReal;

    @Column(name = "Compartir_Datos_Personales", nullable = false)
    private Boolean compartirDatosPersonales;

    @Column(name = "Fecha_Actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
