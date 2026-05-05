package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Orden de servicio: agrupa los ítems a cobrar por una atención.
 * Puede originarse de una Cita o de una emergencia.
 */
@Entity
@Table(name = "ordenes_servicio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenServicio {

    public enum EstadoOrden {
        PENDIENTE, PAGADA, PARCIAL, ANULADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej: OS-20240001 */
    @Column(name = "numero_orden", nullable = false, unique = true, length = 20)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Origen consulta externa */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    /** Origen emergencia */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_emergencia_id")
    private OrdenAtencionEmergencia ordenEmergencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    /** IGV (18% en Perú) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Trabajador creadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
