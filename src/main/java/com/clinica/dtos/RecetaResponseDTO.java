package com.clinica.dtos;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecetaResponseDTO {

    private Long id;
    private String numeroReceta;

    private String pacienteNombre;
    private String pacienteDni;

    private String medicoNombre;

    private String indicacionesGenerales;

    private String estado;

    private LocalDateTime fechaEmision;
    private LocalDate fechaVencimiento;

    private List<DetalleRecetaResponseDTO> detalles;

    public boolean isYaDespachada() {
        return "DESPACHADA".equalsIgnoreCase(estado);
    }
}