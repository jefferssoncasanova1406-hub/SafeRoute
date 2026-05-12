package com.upc.av_2.repositories;

import com.upc.av_2.entidades.ZonaRiesgo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaRiesgoRepository extends JpaRepository<ZonaRiesgo, Integer> {

    List<ZonaRiesgo> findByEstado(Boolean estado);

    List<ZonaRiesgo> findByNivelRiesgo(Integer nivelRiesgo);

    List<ZonaRiesgo> findByEstadoAndNivelRiesgo(Boolean estado, Integer nivelRiesgo);

    List<ZonaRiesgo> findByEstadoTrueAndUbicacionIdUbicacionInOrderByNivelRiesgoDescFechaActualizacionDesc(
            List<Integer> ubicacionIds);
}
