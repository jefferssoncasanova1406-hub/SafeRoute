package com.upc.grupo3.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Ruta_Segura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer idRuta;

    @Column(name = "origen_latitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal origenLatitud;

    @Column(name = "origen_longitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal origenLongitud;

    @Column(name = "destino_latitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinoLatitud;

    @Column(name = "destino_longitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinoLongitud;

    @Column(name = "nivel_riesgo", nullable = false)
    private Integer nivelRiesgo;

    @Column(name = "distancia_metros", nullable = false)
    private Integer distanciaMetros;

    @Column(name = "tiempo_estimado_minutos", nullable = false)
    private Integer tiempoEstimadoMinutos;

    @Column(name = "geometria_geojson", nullable = false, columnDefinition = "TEXT")
    private String geometriaGeojson;

    @Column(name = "resultado_json", columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(name = "fecha_calculo", nullable = false)
    private LocalDateTime fechaCalculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Usuario_ID_Usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "ruta", fetch = FetchType.LAZY)
    @Builder.Default
    private List<RutaZona> rutasZona = new ArrayList<>();
}
