package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "especialidades",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_especialidades_nombre", columnNames = "nombre")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}