package com.clinica.dtos;

import com.clinica.model.entities.SerieComprobante;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SerieComprobanteRequestDTO {

    @NotNull(message = "El tipo de comprobante es obligatorio.")
    private SerieComprobante.TipoComprobante tipoComprobante;

    @NotBlank(message = "El prefijo de la serie es obligatorio.")
    @Size(min = 4, max = 4, message = "El prefijo debe tener 4 caracteres.")
    @Pattern(regexp = "^[A-Za-z][0-9]{3}$", message = "El prefijo debe tener una letra y tres numeros. Ejemplo: B001.")
    private String prefijo;

    @NotNull(message = "El numero inicial es obligatorio.")
    @Min(value = 1, message = "El numero inicial debe ser mayor o igual a 1.")
    @Max(value = 999999, message = "El numero inicial no puede superar 999999.")
    private Integer numeroInicial;

    private Boolean activo;
}
