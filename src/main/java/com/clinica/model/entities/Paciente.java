package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "pacientes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pacientes_dni",   columnNames = "dni"),
        @UniqueConstraint(name = "uk_pacientes_email", columnNames = "email")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 12)
    @Column(nullable = false, length = 12)
    private String dni;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** M / F / OTRO */
    @Column(length = 10)
    private String genero;

    @Column(length = 15)
    private String telefono;

    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    /** Ej: O+, A-, AB+ */
    @Column(name = "tipo_sangre", length = 5)
    private String tipoSangre;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    /**
     * Referencia lógica a HistoriaClinica sin modificar esa entidad.
     * Se resuelve en capa de servicio cuando se necesite navegar.
     */
    @Column(name = "historia_clinica_id")
    private Long historiaClinicaId;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Trabajador registradoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
