package com.clinica.dtos;

import lombok.*;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriaClinicaResponseDTO {
 
    private Long id;
    private String numeroHistoria;
    private String dniPaciente;
    private String nombreCompleto;
    private String telefono;
    private String email;
    private String fechaNacimiento;
    private String genero;
    private String direccion;
    private String creadoPor;
    private LocalDateTime createdAt;
 
    private String redirectUrl;
    private boolean nuevaHistoria;
}