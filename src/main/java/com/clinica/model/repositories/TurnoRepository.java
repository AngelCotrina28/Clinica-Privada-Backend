package com.clinica.model.repositories;

import com.clinica.model.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByMedicoIdAndActivoTrue(Long medicoId);
}
