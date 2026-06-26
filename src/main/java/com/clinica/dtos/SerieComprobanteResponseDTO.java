package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SerieComprobanteResponseDTO {
    private Long id;
    private String tipoComprobante;
    private String prefijo;
    private Integer correlativoActual;
    private Integer siguienteCorrelativo;
    private String siguienteNumero;
    private boolean activo;
}
