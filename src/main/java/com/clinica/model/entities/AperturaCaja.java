package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aperturas_caja")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AperturaCaja {

    public enum EstadoCaja {
        ABIERTA, CERRADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Trabajador cajero;

    @NotNull
    @Column(name = "monto_apertura", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "monto_cierre", precision = 10, scale = 2)
    private BigDecimal montoCierre;

    /** Diferencia entre monto esperado y monto real al cierre */
    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "fecha_apertura", nullable = false)
    @Builder.Default
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
