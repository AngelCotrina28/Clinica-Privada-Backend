package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AperturaCajaResponseDTO {
    private Long id;
    private Long cajaId;
    private String cajaNombre;
    private Long cajeroId;
    private String cajeroNombre;
    private String cajeroUsername;
    private BigDecimal montoInicial;
    private BigDecimal montoCierre;
    private BigDecimal diferencia;
    private String estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjetas;
    private BigDecimal totalBilleteras;
    private BigDecimal totalIngresos;
    private BigDecimal totalTeorico;
}
