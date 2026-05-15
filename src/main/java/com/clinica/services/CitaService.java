package com.clinica.services;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import java.util.List;

public interface CitaService {
    CitaResponseDTO programarCita(CitaRequestDTO request);

    List<CitaResponseDTO> listarCitas();
}