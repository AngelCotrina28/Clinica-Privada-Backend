package com.clinica.model.repositories;

import com.clinica.model.entities.OrdenEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdenEntregaRepository extends JpaRepository<OrdenEntrega, Long> {
    Optional<OrdenEntrega> findFirstByOrderByIdDesc();
}
