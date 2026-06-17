package com.clinica.model.repositories;

import com.clinica.model.entities.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

        boolean existsByNumeroCita(String numeroCita);

        Optional<Cita> findByNumeroCita(String numeroCita);

        boolean existsByMedicoIdAndFechaHoraCitaAndEstadoIn(
                Long medicoId,
                LocalDateTime fechaHoraCita,
                Collection<Cita.EstadoCita> estados);

        List<Cita> findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(
                Long medicoId,
                LocalDateTime inicio,
                LocalDateTime fin,
                Collection<Cita.EstadoCita> estados);
        
        List<Cita> findByHistoriaClinicaIdAndEstado(
                Long historiaClinicaId,
                Cita.EstadoCita estado);

}