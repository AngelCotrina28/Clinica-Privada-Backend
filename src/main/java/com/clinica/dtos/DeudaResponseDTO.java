package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DeudaResponseDTO {
    private Long id;
    private String numeroOrden;
    private String concepto;
    private String conceptoLabel;
    private BigDecimal monto;
    private String estado;
    private Long pacienteId;
    private String pacienteDni;
    private String pacienteNombre;
    private String origenCodigo;
    private LocalDateTime fechaGeneracion;
}
