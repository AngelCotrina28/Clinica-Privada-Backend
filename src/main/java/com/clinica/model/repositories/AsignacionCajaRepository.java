package com.clinica.model.repositories;

import com.clinica.model.entities.AsignacionCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionCajaRepository extends JpaRepository<AsignacionCaja, Long> {

    List<AsignacionCaja> findAllByOrderByIdDesc();

    Optional<AsignacionCaja> findFirstByCajeroIdAndActivoTrueOrderByIdDesc(Long cajeroId);

    boolean existsByCajeroIdAndActivoTrue(Long cajeroId);

    boolean existsByCajeroIdAndActivoTrueAndIdNot(Long cajeroId, Long id);
}
