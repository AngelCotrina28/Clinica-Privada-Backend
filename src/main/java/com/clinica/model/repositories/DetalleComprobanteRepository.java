package com.clinica.model.repositories;

import com.clinica.model.entities.DetalleComprobante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleComprobanteRepository extends JpaRepository<DetalleComprobante, Long> {
}
