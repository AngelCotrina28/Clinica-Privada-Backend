package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Catálogo de diagnósticos CIE-10 (OMS).
 * Se importa masivamente desde CSV del MINSA/OMS.
 */
@Entity
@Table(
    name = "diagnosticos_cie",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cie_codigo", columnNames = "codigo")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiagnosticoCIE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código CIE-10, ej: J06.9, A09, K29.7 */
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String codigo;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 100)
    private String categoria;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
