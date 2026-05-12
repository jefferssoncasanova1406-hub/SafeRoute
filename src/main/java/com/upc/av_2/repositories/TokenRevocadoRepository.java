package com.upc.av_2.repositories;

import com.upc.av_2.entidades.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByTokenHash(String tokenHash);
}
