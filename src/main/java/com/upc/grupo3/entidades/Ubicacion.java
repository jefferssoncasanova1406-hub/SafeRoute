package com.upc.grupo3.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Ubicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ubicacion extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Ubicacion")
    private Integer idUbicacion;

    @Column(name = "latitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "distrito", nullable = false, length = 100)
    private String distrito;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @OneToMany(mappedBy = "ubicacion", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ZonaRiesgo> zonasRiesgo = new ArrayList<>();
}
