package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicamentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Regla: código único y obligatorio */
    @NotBlank(message = "El código no puede estar vacío")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    /** Regla: nombre obligatorio */
    @NotBlank(message = "El nombre del medicamento es obligatorio")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String nombre;

    @Size(max = 200)
    @Column(name = "nombre_generico", length = 200)
    private String nombreGenerico;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    @NotNull(message = "La categoría es obligatoria")
    private CategoriaMedicamento categoria;

    @Size(max = 100)
    @Column(length = 100)
    private String presentacion;

    @Size(max = 150)
    @Column(length = 150)
    private String laboratorio;

    /** Regla: precio > 0 */
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /** Regla: stock >= 0 y entero */
    @Min(value = 0, message = "El stock inicial debe ser mayor o igual a 0")
    @Column(name = "stock_actual", nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    @Min(value = 0, message = "El stock mínimo debe ser mayor o igual a 0")
    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;
    
    @Column(name = "requiere_receta", nullable = false)
    @Builder.Default
    private boolean requiereReceta = false;

    /** Soft-delete: activo/inactivo */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Trabajador createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Trabajador updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}