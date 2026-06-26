package com.clinica.model.repositories;

import com.clinica.model.entities.Caja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    List<Caja> findByActivoTrueOrderByNombreAsc();
}
