package com.clinica.model.repositories;

import com.clinica.model.entities.DetalleOrdenEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleOrdenEntregaRepository extends JpaRepository<DetalleOrdenEntrega, Long> {
}
