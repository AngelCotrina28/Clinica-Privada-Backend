package com.clinica.backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "categorias_medicamento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}