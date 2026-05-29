package com.upc.grupo3.repositories;

import com.upc.grupo3.entidades.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByTokenHash(String tokenHash);
}
