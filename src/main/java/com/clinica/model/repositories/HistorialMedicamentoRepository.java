package com.clinica.model.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clinica.model.entities.HistorialMedicamento;

@Repository
public interface HistorialMedicamentoRepository extends JpaRepository<HistorialMedicamento, Long> {
    Page<HistorialMedicamento> findByMedicamentoIdOrderByFechaOperacionDesc(Long medicamentoId, Pageable pageable);
}
