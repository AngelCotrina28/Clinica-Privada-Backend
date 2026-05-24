package com.clinica.services;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.repositories.AtencionMedicaRepository;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.TrabajadorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtencionMedicaService {

    private final AtencionMedicaRepository atencionRepo;
    private final HistoriaClinicaRepository historiaRepo;
    private final TrabajadorRepository trabajadorRepo;

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

    /**
     * Registra una nueva atención médica en la base de datos.
     */
    @Transactional
    public Long registrarAtencion(com.clinica.dtos.AtencionMedicaRequestDTO request) {
        com.clinica.model.entities.HistoriaClinica historia = historiaRepo.findById(request.getHistoriaClinicaId())
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada con ID: " + request.getHistoriaClinicaId()));

        com.clinica.model.entities.Trabajador medico = trabajadorRepo.findById(request.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado con ID: " + request.getMedicoId()));

        AtencionMedica nuevaAtencion = AtencionMedica.builder()
                .historiaClinica(historia)
                .medico(medico)
                .diagnosticoPrincipal(request.getDiagnosticoPrincipal())
                .observaciones(request.getNotasEvolucion())
                .build();

        AtencionMedica atencionGuardada = atencionRepo.save(nuevaAtencion);

        return atencionGuardada.getId();
    }
}