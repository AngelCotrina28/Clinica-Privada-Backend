package com.clinica.model.repositories;

import com.clinica.model.entities.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    Optional<Comprobante> findByNumeroCompleto(String numeroCompleto);
    List<Comprobante> findAllByOrderByFechaEmisionDesc();
}
