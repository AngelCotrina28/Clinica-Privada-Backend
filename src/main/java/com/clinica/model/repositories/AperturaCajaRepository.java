package com.clinica.model.repositories;

import com.clinica.model.entities.AperturaCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AperturaCajaRepository extends JpaRepository<AperturaCaja, Long> {

    Optional<AperturaCaja> findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(
            Long cajeroId,
            AperturaCaja.EstadoCaja estado);

    Optional<AperturaCaja> findFirstByCajeroIdAndEstadoInOrderByFechaAperturaDesc(
            Long cajeroId,
            List<AperturaCaja.EstadoCaja> estados);

    List<AperturaCaja> findByEstadoOrderByFechaAperturaDesc(AperturaCaja.EstadoCaja estado);
}
