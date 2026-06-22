package com.clinica.model.repositories;

import com.clinica.model.entities.OrdenEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdenEntregaRepository extends JpaRepository<OrdenEntrega, Long> {
    Optional<OrdenEntrega> findFirstByOrderByIdDesc();
}
