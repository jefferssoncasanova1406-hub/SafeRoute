package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.IncidenteCiudadano;
import com.upc.grupo3.entidades.Usuario;
import com.upc.grupo3.entidades.VerificacionComunitaria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionComunitariaRepository extends JpaRepository<VerificacionComunitaria, Integer> {

    boolean existsByIncidenteAndUsuario(IncidenteCiudadano incidente, Usuario usuario);

    long countByIncidenteAndVerificado(IncidenteCiudadano incidente, Boolean verificado);
}
