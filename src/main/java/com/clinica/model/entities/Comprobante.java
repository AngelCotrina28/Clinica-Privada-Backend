package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "comprobantes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_comprobante_numero", columnNames = "numero_completo")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comprobante {

    public enum EstadoComprobante {
        EMITIDO, ANULADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serie_comprobante_id", nullable = false)
    private SerieComprobante serieComprobante;

    @Column(nullable = false)
    private Integer correlativo;

    /** Número completo, ej: B001-00000001 */
    @Column(name = "numero_completo", nullable = false, length = 20)
    private String numeroCompleto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_servicio_id", nullable = false)
    private OrdenServicio ordenServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apertura_caja_id")
    private AperturaCaja aperturaCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** DNI o RUC del receptor */
    @Column(name = "ruc_dni", length = 15)
    private String rucDni;

    @Column(name = "razon_social_nombre", length = 200)
    private String razonSocialNombre;

    @Column(name = "direccion_fiscal", length = 300)
    private String direccionFiscal;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoComprobante estado = EstadoComprobante.EMITIDO;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por", nullable = false)
    private Trabajador emitidoPor;

    @CreationTimestamp
    @Column(name = "fecha_emision", updatable = false)
    private LocalDateTime fechaEmision;
}
