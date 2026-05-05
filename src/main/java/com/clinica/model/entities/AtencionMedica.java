package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Registro del acto clínico: anamnesis, examen físico,
 * diagnóstico y tratamiento indicado por el médico.
 */
@Entity
@Table(name = "atenciones_medicas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtencionMedica {

    public enum EstadoAtencion {
        EN_PROCESO, FINALIZADA, ANULADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Origen: consulta externa */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    /** Origen: emergencia */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_emergencia_id")
    private OrdenAtencionEmergencia ordenEmergencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Trabajador medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    /** Triaje registrado previo a esta atención */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triaje_id")
    private Triaje triaje;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    /** Historia de la enfermedad actual */
    @Column(columnDefinition = "TEXT")
    private String anamnesis;

    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;

    @Column(name = "diagnostico_principal", length = 500)
    private String diagnosticoPrincipal;

    /** Código CIE-10 del diagnóstico principal */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cie10_id")
    private DiagnosticoCIE cie10;

    @Column(name = "diagnostico_secundario", columnDefinition = "TEXT")
    private String diagnosticoSecundario;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoAtencion estado = EstadoAtencion.EN_PROCESO;

    @Column(name = "fecha_hora_inicio")
    @Builder.Default
    private LocalDateTime fechaHoraInicio = LocalDateTime.now();

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;
}
