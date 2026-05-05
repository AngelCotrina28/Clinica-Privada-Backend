package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventario {

    public enum TipoMovimiento {
        ENTRADA,
        SALIDA_DESPACHO,
        AJUSTE_POSITIVO,
        AJUSTE_NEGATIVO,
        VENCIMIENTO,
        DEVOLUCION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "stock_nuevo", nullable = false)
    private Integer stockNuevo;

    /** Despacho que originó la salida (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_despacho_id")
    private DetalleDespacho detalleDespacho;

    /** Lote afectado (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteMedicamento lote;

    @Column(length = 300)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @Column(name = "fecha_movimiento", nullable = false)
    @Builder.Default
    private LocalDateTime fechaMovimiento = LocalDateTime.now();
}
