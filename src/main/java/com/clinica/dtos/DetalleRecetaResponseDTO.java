package com.clinica.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleRecetaResponseDTO {

    private Long id;
    private Long medicamentoId;
    private String medicamentoNombre;
    private String presentacion;
    private String dosis;
    private String frecuencia;
    private String duracion;
    private Integer cantidadPrescrita;
    private Integer cantidadDespachada;
    private String viaAdministracion;
    private String indicaciones;
}