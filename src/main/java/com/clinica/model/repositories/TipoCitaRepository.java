package com.clinica.model.repositories;

import com.clinica.model.entities.TipoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoCitaRepository extends JpaRepository<TipoCita, Long> {
    Optional<TipoCita> findFirstByNombreIgnoreCaseAndActivoTrue(String nombre);

    Optional<TipoCita> findFirstByActivoTrueOrderByIdAsc();
}
