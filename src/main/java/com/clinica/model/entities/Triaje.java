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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_emergencia_id")
    private OrdenAtencionEmergencia ordenEmergencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enfermero_id", nullable = false)
    private Trabajador enfermero;

    @Column(name = "presion_sistolica")
    private Integer presionSistolica;

    @Column(name = "presion_diastolica")
    private Integer presionDiastolica;

    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria")
    private Integer frecuenciaRespiratoria;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperatura;

    @Column(name = "saturacion_oxigeno")
    private Integer saturacionOxigeno;

    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(precision = 5, scale = 2)
    private BigDecimal talla;

    @Column(precision = 4, scale = 2)
    private BigDecimal imc;

    @Column(name = "escala_glasgow")
    private Integer glasgow;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
