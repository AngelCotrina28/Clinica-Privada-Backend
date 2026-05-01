package com.clinica.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO de entrada para generar una Orden de Atención de Emergencia.
 * Roles permitidos: JEFE_ENFERMERIA, ADMINISTRADOR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerarOrdenEmergenciaRequestDTO {

    @NotNull(message = "El ID de la historia clínica es obligatorio")
    private Long historiaClinicaId;

    @NotNull(message = "Debe asignar un médico")
    private Long medicoId;

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String motivo;
}