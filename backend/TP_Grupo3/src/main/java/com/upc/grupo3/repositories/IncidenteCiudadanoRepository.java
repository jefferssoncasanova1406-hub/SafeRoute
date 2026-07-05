package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.EstadoModeracionIncidente;
import com.upc.grupo3.entidades.IncidenteCiudadano;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidenteCiudadanoRepository
        extends JpaRepository<IncidenteCiudadano, Integer>, JpaSpecificationExecutor<IncidenteCiudadano> {

    List<IncidenteCiudadano> findAllByEstadoModeracionOrderByFechaEmisionDesc(
            EstadoModeracionIncidente estadoModeracion);
}
