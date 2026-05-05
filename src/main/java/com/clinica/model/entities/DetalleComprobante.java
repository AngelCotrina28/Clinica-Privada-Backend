package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Líneas del comprobante. Son inmutables una vez emitido;
 * se copian desde DetalleOrdenServicio al momento de la emisión.
 */
@Entity
@Table(name = "detalle_comprobante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
