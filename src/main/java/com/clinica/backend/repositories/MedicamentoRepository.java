package com.clinica.backend.repositories;

import com.clinica.backend.entities.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    /** Verifica si ya existe un código (para validar unicidad en registro) */
    boolean existsByCodigo(String codigo);

    /** Verifica unicidad en edición (excluye el propio registro) */
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Optional<Medicamento> findByCodigo(String codigo);

    /**
     * Búsqueda paginada con filtros opcionales:
     * nombre, código, categoría y estado activo/inactivo.
     */
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

    /** Medicamentos con stock por debajo del mínimo */
    @Query("SELECT m FROM Medicamento m WHERE m.activo = TRUE AND m.stockActual <= m.stockMinimo")
    Page<Medicamento> findStockBajo(Pageable pageable);
}