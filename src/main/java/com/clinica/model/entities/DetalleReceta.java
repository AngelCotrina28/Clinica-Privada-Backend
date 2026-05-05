package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "detalle_receta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    /** Ej: 500mg, 1 tableta */
    @Column(length = 100)
    private String dosis;

    /** Ej: Cada 8 horas, 3 veces al día */
    @Column(length = 100)
    private String frecuencia;

    /** Ej: 7 días, 2 semanas */
    @Column(length = 100)
    private String duracion;

    @Min(1)
    @Column(name = "cantidad_prescrita", nullable = false)
    private Integer cantidadPrescrita;

    @Min(0)
    @Column(name = "cantidad_despachada", nullable = false)
    @Builder.Default
    private Integer cantidadDespachada = 0;

    /** Ej: Oral, Intravenosa, Tópica */
    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion;

    @Column(columnDefinition = "TEXT")
    private String indicaciones;
}
