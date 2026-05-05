package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_medicamento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoteMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @NotBlank
    @Size(max = 50)
    @Column(name = "numero_lote", nullable = false, length = 50)
    private String numeroLote;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Min(0)
    @Column(name = "stock_lote", nullable = false)
    @Builder.Default
    private Integer stockLote = 0;

    @Column(length = 150)
    private String proveedor;

    @Column(name = "precio_compra", precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
