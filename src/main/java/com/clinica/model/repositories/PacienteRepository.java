package com.clinica.model.repositories;

import com.clinica.model.entities.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    
    Optional<Paciente> findByHistoriaClinicaId(Long historiaClinicaId);
}
