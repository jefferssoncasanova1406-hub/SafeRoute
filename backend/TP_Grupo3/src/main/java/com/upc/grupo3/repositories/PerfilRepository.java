package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.Perfil;
import com.upc.grupo3.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    Optional<Perfil> findByUsuario(Usuario usuario);
}
