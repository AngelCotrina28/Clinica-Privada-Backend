package com.clinica.model.repositories;

import com.clinica.model.entities.TipoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoCitaRepository extends JpaRepository<TipoCita, Long> {
}
