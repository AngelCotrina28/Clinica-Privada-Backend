package com.clinica.model.repositories;

import com.clinica.model.entities.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    Optional<Receta> findByNumeroReceta(String numeroReceta);

    @Query("SELECT r FROM Receta r " +
           "WHERE UPPER(r.numeroReceta) = UPPER(:termino) " +
           "OR r.paciente.dni = :termino " +
           "ORDER BY r.fechaEmision DESC")
    List<Receta> buscarPorNumeroRecetaODni(@Param("termino") String termino);

    Optional<Receta> findFirstByOrderByIdDesc();
}