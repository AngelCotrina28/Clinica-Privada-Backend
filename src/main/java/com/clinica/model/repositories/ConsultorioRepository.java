package com.clinica.model.repositories;

import com.clinica.model.entities.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {
    
    Optional<Consultorio> findFirstByEspecialidadIdAndActivoTrueOrderByIdAsc(Long especialidadId);
    Optional<Consultorio> findFirstByActivoTrueOrderByIdAsc();
    List<Consultorio> findByEspecialidadIdAndActivoTrueOrderByIdAsc(Long especialidadId);
    List<Consultorio> findByEspecialidadIsNullAndActivoTrueOrderByIdAsc();
    List<Consultorio> findByActivoTrueOrderByIdAsc();
}
