package com.clinica.model.repositories;

import com.clinica.model.entities.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    boolean existsByMedicoIdAndFechaHoraCita(Long medicoId, LocalDateTime fechaHoraCita);

    boolean existsByMedicoIdAndFechaHoraCitaAndEstadoIn(
            Long medicoId,
            LocalDateTime fechaHoraCita,
            Collection<Cita.EstadoCita> estados);

    List<Cita> findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(
            Long medicoId,
            LocalDateTime inicio,
            LocalDateTime fin,
            Collection<Cita.EstadoCita> estados);
}
