package com.clinica.services;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.DisponibilidadResponseDTO;
import java.util.List;

public interface CitaService {
    CitaResponseDTO programarCita(CitaRequestDTO request);

    List<CitaResponseDTO> listarCitas();

    List<DisponibilidadResponseDTO> consultarDisponibilidad(Long medicoId,
            java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);
}