package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Orden de Atención de Emergencia.
 * Vincula una Historia Clínica con un Médico disponible.
 */
@Entity
@Table(name = "ordenes_atencion_emergencia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenAtencionEmergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de orden generado (ej. OE-20240001) */
    @Column(name = "numero_orden", nullable = false, unique = true, length = 20)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    /** Médico asignado para la atención */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Trabajador medico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    /** Trabajador que generó la orden */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generado_por", nullable = false)
    private Trabajador generadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EstadoOrden {
        PENDIENTE, EN_ATENCION, FINALIZADO, CANCELADO
    }
}