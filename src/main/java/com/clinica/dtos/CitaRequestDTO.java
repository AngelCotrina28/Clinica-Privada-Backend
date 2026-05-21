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
    // Los datos que vienen del frontend
    private Long pacienteId;
    private Long especialidadId;
    private Long medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;

    // Campos obligatorios por la BD
    private Long consultorioId;
    private Long tipoCitaId;
    private Long creadoPorId;
}