package com.clinica.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadResponseDTO {

    private LocalDate fecha;
    private String diaNombre;
    private List<String> horariosDisponibles;
}