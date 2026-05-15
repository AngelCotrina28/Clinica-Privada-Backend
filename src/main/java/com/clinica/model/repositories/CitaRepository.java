package com.clinica.model.repositories;

import com.clinica.model.entities.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    boolean existsByMedicoIdAndFechaHoraCita(Long medicoId, java.time.LocalDateTime fechaHoraCita);
}