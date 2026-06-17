package com.clinica.model.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clinica.model.entities.Medicamento;

import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Optional<Medicamento> findByCodigo(String codigo);

    @Query("""
        SELECT m FROM Medicamento m
        JOIN FETCH m.categoria c
        WHERE (:nombre    IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
          AND (:codigo    IS NULL OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', :codigo, '%')))
          AND (:catId     IS NULL OR c.id = :catId)
          AND (:soloActivos = FALSE OR m.activo = TRUE)
        """)
    Page<Medicamento> buscar(
            @Param("nombre") String nombre,
            @Param("codigo") String codigo,
            @Param("catId")  Integer catId,
            @Param("soloActivos") boolean soloActivos,
            Pageable pageable);

    @Query("SELECT m FROM Medicamento m WHERE m.activo = TRUE AND m.stockActual <= m.stockMinimo")
    Page<Medicamento> findStockBajo(Pageable pageable);
}