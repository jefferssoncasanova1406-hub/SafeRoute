package com.upc.av_2.entidades;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
public class RutaZona {

    @EmbeddedId
    private RutaZonaId id;
}
