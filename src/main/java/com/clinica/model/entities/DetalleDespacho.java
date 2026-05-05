package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;


@Entity
@Table(name = "detalle_despacho")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "despacho_farmacia_id", nullable = false)
    private DespachoFarmacia despachoFarmacia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_receta_id", nullable = false)
    private DetalleReceta detalleReceta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    /** Lote del que se extrae el medicamento (trazabilidad) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteMedicamento lote;

    @Min(1)
    @Column(name = "cantidad_despachada", nullable = false)
    private Integer cantidadDespachada;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
