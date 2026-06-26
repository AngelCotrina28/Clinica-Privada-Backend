package com.clinica.dtos;

import com.clinica.model.entities.Pago;
import com.clinica.model.entities.SerieComprobante;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PagoRequestDTO {

    @NotEmpty(message = "Debe seleccionar al menos una deuda.")
    private List<Long> deudaIds;

    @NotNull(message = "El metodo de pago es obligatorio.")
    private Pago.MetodoPago metodoPago;

    @NotNull(message = "El tipo de comprobante es obligatorio.")
    private SerieComprobante.TipoComprobante tipoComprobante;

    @Size(max = 15, message = "El RUC/DNI no puede superar 15 caracteres.")
    private String rucDni;

    @Size(max = 200, message = "La razon social o nombre no puede superar 200 caracteres.")
    private String razonSocialNombre;

    @Size(max = 300, message = "La direccion fiscal no puede superar 300 caracteres.")
    private String direccionFiscal;

    @Size(max = 100, message = "La referencia no puede superar 100 caracteres.")
    private String referencia;
}
