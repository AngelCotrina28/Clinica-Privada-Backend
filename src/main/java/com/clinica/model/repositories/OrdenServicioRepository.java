package com.clinica.model.repositories;

import com.clinica.model.entities.OrdenServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Long> {

    boolean existsByNumeroOrden(String numeroOrden);

    Optional<OrdenServicio> findFirstByOrderByIdDesc();

    List<OrdenServicio> findByIdIn(Collection<Long> ids);

    List<OrdenServicio> findByPacienteDniAndEstadoOrderByCreatedAtDesc(
            String dni,
            OrdenServicio.EstadoOrden estado);

    Optional<OrdenServicio> findFirstByCitaIdOrderByIdDesc(Long citaId);

    Optional<OrdenServicio> findFirstByOrdenEmergenciaIdOrderByIdDesc(Long ordenEmergenciaId);

    List<OrdenServicio> findByPacienteIdAndObservacionesContainingIgnoreCaseOrderByIdDesc(
            Long pacienteId,
            String observaciones);
}
