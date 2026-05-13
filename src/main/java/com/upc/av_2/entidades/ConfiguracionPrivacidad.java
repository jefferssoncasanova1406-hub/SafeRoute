package com.upc.av_2.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "ID_Usuario", nullable = false, unique = true)
    private Integer idUsuario;

    @Column(name = "Ubicacion_Tiempo_Real", nullable = false)
    private Boolean ubicacionTiempoReal;

    @Column(name = "Compartir_Datos_Personales", nullable = false)
    private Boolean compartirDatosPersonales;

    @Column(name = "Fecha_Actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
