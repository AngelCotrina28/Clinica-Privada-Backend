package com.clinica.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtencionMedicaHistorialDTO {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private String medicoNombre;
    private String motivoConsulta;
    private String anamnesis;
    private String examenFisico;
    private String diagnosticoPrincipal;
    private String diagnosticoSecundario;
    private String tratamiento;
}