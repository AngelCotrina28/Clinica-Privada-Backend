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

    private Long pacienteId;
    private Long historiaClinicaId;

    private Long especialidadId;
    private Long medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta;

    private Long turnoId;

    private Long consultorioId;
    private Long tipoCitaId;
    private Long creadoPorId;
}