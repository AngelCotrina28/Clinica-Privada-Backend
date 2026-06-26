package com.clinica.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AsignacionCajaRequestDTO {

    @NotNull(message = "El cajero es obligatorio.")
    private Long cajeroId;

    private Long cajaId;

    @Size(max = 100, message = "El nombre de caja no puede superar 100 caracteres.")
    private String cajaNombre;

    @Size(max = 200, message = "La ubicacion no puede superar 200 caracteres.")
    private String cajaUbicacion;

    @NotNull(message = "La serie de boleta es obligatoria.")
    private Long serieBoletaId;

    @NotNull(message = "La serie de factura es obligatoria.")
    private Long serieFacturaId;

    private Boolean activo;
}
