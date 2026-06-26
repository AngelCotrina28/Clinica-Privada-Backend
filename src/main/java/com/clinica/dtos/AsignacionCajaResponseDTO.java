package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AsignacionCajaResponseDTO {
    private Long id;
    private Long cajeroId;
    private String cajeroNombre;
    private String cajeroUsername;
    private Long cajaId;
    private String cajaNombre;
    private String cajaUbicacion;
    private Long serieBoletaId;
    private String serieBoleta;
    private Long serieFacturaId;
    private String serieFactura;
    private boolean activo;
    private LocalDateTime fechaAsignacion;
}
