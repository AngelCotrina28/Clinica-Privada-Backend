package com.clinica.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnularComprobanteRequestDTO {

    @NotBlank(message = "El motivo de anulacion es obligatorio.")
    @Size(max = 500, message = "El motivo no puede superar 500 caracteres.")
    private String motivo;
}
