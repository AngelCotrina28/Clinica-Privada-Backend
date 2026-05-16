package com.clinica.services;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.HorarioBloqueDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import java.time.LocalDate;
import java.util.List;

public interface CitaService {
    CitaResponseDTO programarCita(CitaRequestDTO request);

    List<CitaResponseDTO> listarCitas();

    List<TrabajadorResponseDTO> listarMedicosPorEspecialidad(Long especialidadId);

    List<HorarioBloqueDTO> obtenerDisponibilidad(Long medicoId, LocalDate fecha);
}
