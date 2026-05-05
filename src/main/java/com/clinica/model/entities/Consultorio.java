package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "consultorios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 10)
    private String numero;

    @Column(length = 20)
    private String piso;

    /** Especialidad principal del consultorio (opcional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
