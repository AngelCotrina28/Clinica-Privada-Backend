package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "cajas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 200)
    private String ubicacion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
