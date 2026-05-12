package com.upc.av_2.repositories;

import com.upc.av_2.entidades.ConfiguracionPrivacidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionPrivacidadRepository extends JpaRepository<ConfiguracionPrivacidad, Integer> {

    Optional<ConfiguracionPrivacidad> findByIdUsuario(Integer idUsuario);
}
