package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "servicios",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_servicios_codigo", columnNames = "codigo")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String codigo;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servicio_id", nullable = false)
    private TipoServicio tipoServicio;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
