package com.upc.av_2.repositories;

import com.upc.av_2.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    boolean existsByEmailIgnoreCase(String email);
}
