package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Turno {

    public enum DiaSemana {
        LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Trabajador medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false)
    private Consultorio consultorio;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Min(1)
    @Column(name = "cupo_maximo", nullable = false)
    @Builder.Default
    private Integer cupoMaximo = 20;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
