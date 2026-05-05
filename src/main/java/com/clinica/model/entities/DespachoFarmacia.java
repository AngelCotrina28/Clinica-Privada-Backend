package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "despachos_farmacia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DespachoFarmacia {

    public enum EstadoDespacho {
        COMPLETO, PARCIAL, CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmaceutico_id", nullable = false)
    private Trabajador farmaceutico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoDespacho estado = EstadoDespacho.COMPLETO;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_despacho", nullable = false)
    @Builder.Default
    private LocalDateTime fechaDespacho = LocalDateTime.now();
}
