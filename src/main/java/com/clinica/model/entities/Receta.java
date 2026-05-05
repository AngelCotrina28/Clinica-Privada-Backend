package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recetas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Receta {

    public enum EstadoReceta {
        EMITIDA, DESPACHADA, PARCIAL, ANULADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número único de receta, ej: RX-20240001 */
    @Column(name = "numero_receta", nullable = false, unique = true, length = 20)
    private String numeroReceta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Trabajador medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "indicaciones_generales", columnDefinition = "TEXT")
    private String indicacionesGenerales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoReceta estado = EstadoReceta.EMITIDA;

    @CreationTimestamp
    @Column(name = "fecha_emision", updatable = false)
    private LocalDateTime fechaEmision;

    /** Fecha hasta la que es válida la receta */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
