package com.clinica.model.repositories;

import com.clinica.model.entities.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    /** Verifica si ya existe una historia para el DNI dado (prevención de duplicados) */
    boolean existsByDniPaciente(String dniPaciente);

    /** Busca la historia por DNI del paciente */
    Optional<HistoriaClinica> findByDniPaciente(String dniPaciente);

    /** Busca por número de historia clínica */
    Optional<HistoriaClinica> findByNumeroHistoria(String numeroHistoria);

    /** Obtiene el último número de historia para auto-incremento */
    long count();
}