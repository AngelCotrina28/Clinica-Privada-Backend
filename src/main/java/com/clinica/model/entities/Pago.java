package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {

    public enum MetodoPago {
        EFECTIVO, TARJETA_DEBITO, TARJETA_CREDITO,
        TRANSFERENCIA, SEGURO, OTRO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_servicio_id", nullable = false)
    private OrdenServicio ordenServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apertura_caja_id", nullable = false)
    private AperturaCaja aperturaCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id")
    private Comprobante comprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** Nro. de operación, voucher, transferencia, etc. */
    @Column(length = 100)
    private String referencia;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Trabajador registradoPor;

    @Column(name = "fecha_pago", nullable = false)
    @Builder.Default
    private LocalDateTime fechaPago = LocalDateTime.now();
}
