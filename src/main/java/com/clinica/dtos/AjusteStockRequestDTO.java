package com.clinica.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteStockRequestDTO {

    @NotNull(message = "La cantidad a ingresar es obligatoria")
    @Min(value = 1, message = "La cantidad a ingresar debe ser mayor a 0")
    private Integer cantidad;

}