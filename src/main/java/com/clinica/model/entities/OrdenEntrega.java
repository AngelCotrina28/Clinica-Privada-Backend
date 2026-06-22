package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "ordenes_entrega",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ordenes_entrega_numero", columnNames = "numero_orden")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden", nullable = false, unique = true, length = 20)
    private String numeroOrden; // Ej: ENT-000001

    // Relación con la receta que originó esta entrega
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "receta_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_ordenes_entrega_receta")
    )
    private Receta receta;

    // Trazabilidad: ¿Qué técnico de farmacia realizó físicamente el despacho?
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "tecnico_id", 
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_ordenes_entrega_tecnico")
    )
    private Trabajador tecnico;

    @CreationTimestamp
    @Column(name = "fecha_entrega", updatable = false)
    private LocalDateTime fechaEntrega;

    @OneToMany(mappedBy = "ordenEntrega", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleOrdenEntrega> detalles = new ArrayList<>();
}
