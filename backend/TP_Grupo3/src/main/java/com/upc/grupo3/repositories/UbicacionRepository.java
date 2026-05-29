package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.Ubicacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Integer> {

    List<Ubicacion> findByCiudadIgnoreCase(String ciudad);

    List<Ubicacion> findByCiudadIgnoreCaseAndDistritoIgnoreCase(String ciudad, String distrito);
}
