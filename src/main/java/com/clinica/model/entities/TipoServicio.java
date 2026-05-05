package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(
    name = "tipos_servicio",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipos_servicio_nombre", columnNames = "nombre")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej: Consulta, Procedimiento, Examen, Medicamento */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
