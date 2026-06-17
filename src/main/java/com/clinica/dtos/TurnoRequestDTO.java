package com.clinica.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TurnoRequestDTO {
    @NotNull(message = "Debe seleccionar un medico")
    private Long medicoId;

    @NotNull(message = "Debe seleccionar una especialidad")
    private Long especialidadId;

    @NotNull(message = "Debe seleccionar una fecha")
    private LocalDate fecha;

    @NotNull(message = "Debe seleccionar hora de inicio")
    private LocalTime horaInicio;

    @NotNull(message = "Debe seleccionar hora de fin")
    private LocalTime horaFin;
}
