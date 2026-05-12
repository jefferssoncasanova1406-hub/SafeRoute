package com.upc.av_2.repositories;

import com.upc.av_2.entidades.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenteRepository extends JpaRepository<Incidente, Integer> {
}
