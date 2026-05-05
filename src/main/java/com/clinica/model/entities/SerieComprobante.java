package com.clinica.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(
    name = "series_comprobante",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_serie_tipo_serie",
            columnNames = {"tipo_comprobante", "serie"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SerieComprobante {

    public enum TipoComprobante {
        BOLETA, FACTURA, NOTA_CREDITO, NOTA_DEBITO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 15)
    private TipoComprobante tipoComprobante;

    /** B001, F001, BC01, etc. */
    @NotBlank
    @Size(max = 4)
    @Column(nullable = false, length = 4)
    private String serie;

    @Min(0)
    @Column(name = "correlativo_actual", nullable = false)
    @Builder.Default
    private Integer correlativoActual = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
