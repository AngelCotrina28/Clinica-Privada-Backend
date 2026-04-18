package com.clinica.backend.repositories;

import com.clinica.backend.entities.CategoriaMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaMedicamentoRepository extends JpaRepository<CategoriaMedicamento, Integer> { }