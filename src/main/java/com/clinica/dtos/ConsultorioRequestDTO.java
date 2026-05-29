package com.clinica.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultorioRequestDTO {

    @NotBlank(message = "El nombre del consultorio es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @Size(max = 10, message = "El numero no puede superar 10 caracteres.")
    private String numero;

    @Size(max = 20, message = "El piso no puede superar 20 caracteres.")
    private String piso;

    private Long especialidadId;
    private Boolean activo;
}
