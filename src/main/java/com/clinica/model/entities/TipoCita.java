package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(
    name = "tipos_cita",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipos_cita_nombre", columnNames = "nombre")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /** Duración estimada de la cita en minutos */
    @Min(1)
    @Column(name = "duracion_minutos", nullable = false)
    @Builder.Default
    private Integer duracionMinutos = 30;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
