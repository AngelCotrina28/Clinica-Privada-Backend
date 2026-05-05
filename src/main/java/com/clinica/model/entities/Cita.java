package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cita {

    public enum EstadoCita {
        PROGRAMADA, CONFIRMADA, ATENDIDA, CANCELADA, NO_ASISTIO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de cita autogenerado, ej: CT-20240001 */
    @Column(name = "numero_cita", nullable = false, unique = true, length = 20)
    private String numeroCita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Trabajador medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false)
    private Consultorio consultorio;

    /** Turno origen de la cita (puede ser nulo si se agenda manualmente) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_cita_id", nullable = false)
    private TipoCita tipoCita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "fecha_hora_cita", nullable = false)
    private LocalDateTime fechaHoraCita;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Trabajador creadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
