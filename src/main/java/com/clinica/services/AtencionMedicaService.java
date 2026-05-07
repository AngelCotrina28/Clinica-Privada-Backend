package com.clinica.services;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.repositories.AtencionMedicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtencionMedicaService {

    private final AtencionMedicaRepository atencionRepo;

    /**
     * Recupera el historial clínico detallado.
     * Mapea cada entidad AtencionMedica a su DTO de historial.
     */
    @Transactional(readOnly = true)
    public List<AtencionMedicaHistorialDTO> obtenerHistorialPorPaciente(Long historiaId) {
        return atencionRepo.findByHistoriaClinicaIdOrderByFechaHoraInicioDesc(historiaId)
                .stream()
                .map(this::mapToHistorialDTO)
                .collect(Collectors.toList());
    }

    private AtencionMedicaHistorialDTO mapToHistorialDTO(AtencionMedica a) {
        return AtencionMedicaHistorialDTO.builder()
                .id(a.getId())
                .fechaHoraInicio(a.getFechaHoraInicio())
                .medicoNombre(a.getMedico().getNombreCompleto())
                .motivoConsulta(a.getMotivoConsulta())
                .anamnesis(a.getAnamnesis())
                .examenFisico(a.getExamenFisico())
                .diagnosticoPrincipal(a.getDiagnosticoPrincipal())
                .diagnosticoSecundario(a.getDiagnosticoSecundario())
                .tratamiento(a.getTratamiento())
                .build();
    }
}