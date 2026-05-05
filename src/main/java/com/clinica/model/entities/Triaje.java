package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Triaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Uno de los dos debe estar presente: cita o emergencia */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_emergencia_id")
    private OrdenAtencionEmergencia ordenEmergencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enfermero_id", nullable = false)
    private Trabajador enfermero;

    /** Presión arterial sistólica (mmHg) */
    @Column(name = "presion_sistolica")
    private Integer presionSistolica;

    /** Presión arterial diastólica (mmHg) */
    @Column(name = "presion_diastolica")
    private Integer presionDiastolica;

    /** Frecuencia cardíaca (bpm) */
    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    /** Frecuencia respiratoria (rpm) */
    @Column(name = "frecuencia_respiratoria")
    private Integer frecuenciaRespiratoria;

    /** Temperatura corporal (°C) */
    @Column(precision = 4, scale = 1)
    private BigDecimal temperatura;

    /** Saturación de oxígeno (%) */
    @Column(name = "saturacion_oxigeno")
    private Integer saturacionOxigeno;

    /** Peso en kilogramos */
    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    /** Talla en centímetros */
    @Column(precision = 5, scale = 2)
    private BigDecimal talla;

    /** Índice de masa corporal (calculado: peso/talla²) */
    @Column(precision = 4, scale = 2)
    private BigDecimal imc;

    /** Escala de Glasgow (3-15) */
    @Column(name = "escala_glasgow")
    private Integer glasgow;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
