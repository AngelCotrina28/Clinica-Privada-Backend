package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class TurnoResponseDTO {
    private Long id;
    private Long medicoId;
    private String nombreMedico;
    private Long especialidadId;
    private String especialidad;
    private Long consultorioId;
    private String consultorio;
    private LocalDate fecha;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean activo;
}
