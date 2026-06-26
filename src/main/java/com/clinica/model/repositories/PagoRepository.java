package com.clinica.model.repositories;

import com.clinica.model.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByAperturaCajaId(Long aperturaCajaId);
    List<Pago> findByComprobanteId(Long comprobanteId);
}
