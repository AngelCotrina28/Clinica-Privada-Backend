package com.clinica.dtos;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TurnoRequestDTO {
    private Long medicoId;
    private Long especialidadId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
