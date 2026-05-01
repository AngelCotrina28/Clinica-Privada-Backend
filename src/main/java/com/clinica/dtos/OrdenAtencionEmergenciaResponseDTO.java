package com.clinica.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenAtencionEmergenciaResponseDTO {

    private Long id;
    private String numeroOrden;

    // Historia Clínica
    private Long historiaClinicaId;
    private String numeroHistoria;
    private String dniPaciente;
    private String nombrePaciente;

    // Médico asignado
    private Long medicoId;
    private String nombreMedico;
    private String especialidadMedico;
    private String estado;
    private String motivo;
    private String generadoPor;
    private LocalDateTime createdAt;
}