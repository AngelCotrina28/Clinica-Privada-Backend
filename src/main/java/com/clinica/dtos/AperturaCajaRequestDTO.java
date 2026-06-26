package com.clinica.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AperturaCajaRequestDTO {

    @NotNull(message = "El monto inicial es obligatorio.")
    @DecimalMin(value = "0.00", message = "El monto inicial debe ser mayor o igual a 0.00.")
    private BigDecimal montoInicial;
}
