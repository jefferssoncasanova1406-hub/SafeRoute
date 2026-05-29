package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.ConfiguracionPrivacidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionPrivacidadRepository extends JpaRepository<ConfiguracionPrivacidad, Integer> {

    Optional<ConfiguracionPrivacidad> findByUsuario_IdUsuario(Integer idUsuario);
}
