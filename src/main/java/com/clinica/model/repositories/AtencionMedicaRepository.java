package com.clinica.model.repositories;

import com.clinica.model.entities.AtencionMedica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {
    List<AtencionMedica> findByHistoriaClinicaIdOrderByFechaHoraInicioDesc(Long historiaClinicaId);
}