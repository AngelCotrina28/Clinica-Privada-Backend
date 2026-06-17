package com.clinica.model.repositories;

import com.clinica.model.entities.OrdenAtencionEmergencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface OrdenAtencionEmergenciaRepository extends JpaRepository<OrdenAtencionEmergencia, Long> {

        boolean existsByNumeroOrden(String numeroOrden);

        Optional<OrdenAtencionEmergencia> findByNumeroOrden(String numeroOrden);
        List<OrdenAtencionEmergencia> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime inicio, LocalDateTime fin);

        List<OrdenAtencionEmergencia> findByHistoriaClinicaIdAndEstado(
        Long historiaClinicaId, OrdenAtencionEmergencia.EstadoOrden estado);

        List<OrdenAtencionEmergencia> findByHistoriaClinicaIdOrderByCreatedAtDesc(Long historiaClinicaId);

        @Query("""
                SELECT o
                FROM OrdenAtencionEmergencia o
                JOIN o.historiaClinica h
                JOIN o.medico m
                WHERE (:inicio IS NULL OR o.createdAt >= :inicio)
                AND (:fin IS NULL OR o.createdAt <= :fin)
                AND (
                        :termino IS NULL
                        OR LOWER(h.nombreCompleto) LIKE CONCAT('%', :termino, '%')
                        OR LOWER(m.nombreCompleto) LIKE CONCAT('%', :termino, '%')
                        )
                ORDER BY o.createdAt DESC
                """)
        Page<OrdenAtencionEmergencia> auditarOrdenes(
                @Param("inicio") LocalDateTime inicio,
                @Param("fin") LocalDateTime fin,
                @Param("termino") String termino,
                Pageable pageable);
}
