package com.clinica.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioBloqueDTO {
    private Long turnoId;
    private Long consultorioId;
    private String consultorio;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean disponible;
    private String estado;
    private Long citaId;
    private String numeroCita;
}
