package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa la Historia Clínica de un Paciente.
 * Un paciente tiene exactamente una historia clínica (identificada por DNI único).
 */
@Entity
@Table(
    name = "historias_clinicas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_historia_dni_paciente", columnNames = "dni_paciente")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de historia clínica generado automáticamente (ej. HC-00001) */
    @Column(name = "numero_historia", nullable = false, unique = true, length = 20)
    private String numeroHistoria;

    // ── Datos del Paciente ───────────────────────────────────────────────────

    @Column(name = "dni_paciente", nullable = false, length = 12)
    private String dniPaciente;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 15)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "fecha_nacimiento", length = 10)
    private String fechaNacimiento;

    @Column(length = 10)
    private String genero;

    @Column(name = "direccion", columnDefinition = "TEXT")
    private String direccion;

    // ── Auditoría ────────────────────────────────────────────────────────────

    /** Trabajador que abrió la historia */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Trabajador creadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}