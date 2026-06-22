package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_orden_entrega")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleOrdenEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "orden_entrega_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_det_entrega_orden")
    )
    private OrdenEntrega ordenEntrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "medicamento_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_det_entrega_medicamento")
    )
    private Medicamento medicamento;

    @Column(name = "cantidad_entregada", nullable = false)
    private Integer cantidadEntregada;
}
