package com.clinica.model.repositories;

import com.clinica.model.entities.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    boolean existsByDniPaciente(String dniPaciente);

    Optional<HistoriaClinica> findByDniPaciente(String dniPaciente);

    Optional<HistoriaClinica> findByNumeroHistoria(String numeroHistoria);

    long count();
}