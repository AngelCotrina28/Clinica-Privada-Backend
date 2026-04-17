package com.clinica.backend.repositories;

import com.clinica.backend.entities.HistorialMedicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialMedicamentoRepository extends JpaRepository<HistorialMedicamento, Long> {
    Page<HistorialMedicamento> findByMedicamentoIdOrderByFechaOperacionDesc(Long medicamentoId, Pageable pageable);
}
