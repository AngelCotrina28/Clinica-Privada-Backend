package com.clinica.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponseDTO {
    private Long id;
    private String numeroCita;
    private String nombrePaciente;
    private String nombreMedico;
    private String consultorio;
    private LocalDateTime fechaHoraCita;
    private String estado;
}