package com.upc.av_2.repositories;

import com.upc.av_2.entidades.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreIgnoreCase(String nombre);
}
