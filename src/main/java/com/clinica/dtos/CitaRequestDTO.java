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
public class CitaRequestDTO {

    // --- Identificadores del Paciente ---
    private Long pacienteId;         
    private Long historiaClinicaId; 

    // --- Datos Generales de la Cita ---
    private Long especialidadId;     
    private Long medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;   // Opcional

    // --- Control de Horarios y Estructura BD ---
    private Long turnoId;        

    // --- Campos Obligatorios de Infraestructura ---
    private Long consultorioId;
    private Long tipoCitaId;
    private Long creadoPorId;
}