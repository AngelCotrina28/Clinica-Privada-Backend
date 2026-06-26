package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ComprobanteResponseDTO {
    private Long id;
    private String numeroCompleto;
    private String tipoComprobante;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    private String pacienteDni;
    private String pacienteNombre;
    private String emitidoPor;
    private String metodoPago;
    private LocalDateTime fechaEmision;
    private List<DeudaResponseDTO> deudas;
}
