package com.clinica.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de entrada para CREAR o EDITAR un medicamento.
 * Las validaciones aquí reflejan las reglas de negocio:
 *   - precio > 0
 *   - código no vacío
 *   - nombre no vacío
 *   - stock >= 0
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicamentoRequestDTO {
 
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    private String codigo;
 
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;
 
    @Size(max = 200)
    private String nombreGenerico;
 
    private String descripcion;
 
    @NotNull(message = "La categoría es obligatoria")
    @Positive(message = "ID de categoría inválido")
    private Integer categoriaId;
 
    @Size(max = 100)
    private String presentacion;
 
    @Size(max = 150)
    private String laboratorio;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
    @Digits(integer = 8, fraction = 2, message = "El precio no puede tener más de 2 decimales")
    private BigDecimal precioUnitario;

    @NotNull(message = "El stock inicial es obligatorio")
    @Min(value = 0, message = "El stock inicial debe ser mayor o igual a 0")
    private Integer stockInicial;

    @Min(value = 0, message = "El stock mínimo debe ser mayor o igual a 0")
    private Integer stockMinimo = 0;

    private boolean requiereReceta = false;
}
