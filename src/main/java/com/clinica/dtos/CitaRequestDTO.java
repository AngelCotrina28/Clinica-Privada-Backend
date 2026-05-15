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
    private Long historiaClinicaId;
    private Long especialidadId; // Lo recibimos aunque la entidad no lo guarde directo, puede servir para lógica
    private Long medicoId;
    private LocalDateTime fechaHora;
    private String motivoConsulta; // Opcional

    // Campos obligatorios por la BD (Los dejamos preparados)
    private Long consultorioId;
    private Long tipoCitaId;
    private Long creadoPorId;
}