package com.clinica.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRecetaRequestDTO {
    
    @NotNull(message = "El ID del medicamento es obligatorio")
    private Long medicamentoId;
    
    @Min(value = 1, message = "Los días deben ser mayor a 0")
    private Integer dias;
    
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
    
    private String indicaciones;
}