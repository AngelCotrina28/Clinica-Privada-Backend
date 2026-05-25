package com.clinica.model.repositories;

import com.clinica.model.entities.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    boolean existsByDniPaciente(String dniPaciente);

    Optional<HistoriaClinica> findByDniPaciente(String dniPaciente);

    Optional<HistoriaClinica> findByNumeroHistoria(String numeroHistoria);

    long count();
}