package com.clinica.model.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_medicamentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialMedicamento {

    public enum TipoOperacion { CREACION, EDICION, INACTIVACION, ACTIVACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", nullable = false, length = 20)
    private TipoOperacion tipoOperacion;

    @Column(name = "campo_modificado", length = 60)
    private String campoModificado;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_operacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaOperacion = LocalDateTime.now();

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;
}