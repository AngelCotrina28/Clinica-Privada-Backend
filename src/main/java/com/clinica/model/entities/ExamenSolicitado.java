package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "examenes_solicitados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamenSolicitado {

    public enum EstadoExamen {
        SOLICITADO, EN_PROCESO, COMPLETADO, CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;

    @Column(name = "tipo_examen", nullable = false, length = 100)
    private String tipoExamen;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoExamen estado = EstadoExamen.SOLICITADO;

    @Column(columnDefinition = "TEXT")
    private String resultados;

    @Column(name = "url_resultado", length = 500)
    private String urlResultado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por", nullable = false)
    private Trabajador solicitadoPor;

    @CreationTimestamp
    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resultado")
    private LocalDateTime fechaResultado;
}
