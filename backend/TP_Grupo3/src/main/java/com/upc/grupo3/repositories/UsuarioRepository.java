package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByResetPasswordToken(String token);

    //Optional<Usuario> findByResetPasswordToken(String token);
}
