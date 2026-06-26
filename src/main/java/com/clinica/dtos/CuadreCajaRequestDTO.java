package com.clinica.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CuadreCajaRequestDTO {

    @NotNull(message = "El dinero contado es obligatorio.")
    @DecimalMin(value = "0.00", message = "El dinero contado debe ser mayor o igual a 0.00.")
    private BigDecimal dineroContado;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres.")
    private String observaciones;
}
