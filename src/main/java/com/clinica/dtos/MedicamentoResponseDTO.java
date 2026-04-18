package com.clinica.backend.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicamentoResponseDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private String nombreGenerico;
    private String descripcion;
    private Integer categoriaId;
    private String categoriaNombre;
    private String presentacion;
    private String laboratorio;
    private BigDecimal precioUnitario;
    private Integer stockActual;
    private Integer stockMinimo;
    private boolean requiereReceta;
    private boolean activo;
    private String creadoPor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Indica si el stock está por debajo del mínimo */
    public boolean isStockBajo() {
        return stockActual != null && stockMinimo != null && stockActual <= stockMinimo;
    }
}