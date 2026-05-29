package com.upc.grupo3.entidades;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Ruta_Segura_Zona")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaZona extends EntidadAuditable {

    @EmbeddedId
    private RutaZonaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idRuta")
    @JoinColumn(name = "id_ruta", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idZona")
    @JoinColumn(name = "id_zona", nullable = false)
    private ZonaRiesgo zona;
}
