package com.clinica.model.repositories;

import com.clinica.model.entities.OrdenAtencionEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenAtencionEmergenciaRepository extends JpaRepository<OrdenAtencionEmergencia, Long> {

    List<OrdenAtencionEmergencia> findByHistoriaClinicaIdOrderByCreatedAtDesc(Long historiaClinicaId);
}

