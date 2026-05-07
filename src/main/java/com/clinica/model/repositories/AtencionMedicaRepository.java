package com.clinica.model.repositories;

import com.clinica.model.entities.AtencionMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {
    /**
     * Obtiene el historial de atenciones medicas de un paciente,
     * ordenado cronologicamente desde el mas reciente al mas antiguo.
     */
    List<AtencionMedica> findByHistoriaClinicaIdOrderByFechaHoraInicioDesc(Long historiaClinicaId);
}