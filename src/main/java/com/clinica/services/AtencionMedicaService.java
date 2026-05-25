package com.clinica.services;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.repositories.AtencionMedicaRepository;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.model.repositories.CitaRepository;
import com.clinica.model.repositories.OrdenAtencionEmergenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtencionMedicaService {

    private final AtencionMedicaRepository atencionRepo;
    private final HistoriaClinicaRepository historiaRepo;
    private final TrabajadorRepository trabajadorRepo;
    private final CitaRepository citaRepo;
    private final OrdenAtencionEmergenciaRepository ordenAtencionEmergenciaRepo;

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

    @Transactional
    public Long registrarAtencion(com.clinica.dtos.AtencionMedicaRequestDTO request) {
        com.clinica.model.entities.HistoriaClinica historia = historiaRepo.findById(request.getHistoriaClinicaId())
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada con ID: " + request.getHistoriaClinicaId()));

        com.clinica.model.entities.Trabajador medico = trabajadorRepo.findById(request.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado con ID: " + request.getMedicoId()));

        AtencionMedica.AtencionMedicaBuilder atencionBuilder = AtencionMedica.builder()
                .historiaClinica(historia)
                .medico(medico)
                .diagnosticoPrincipal(request.getDiagnosticoPrincipal())
                .observaciones(request.getNotasEvolucion());

        String codigo = request.getNumeroCita(); 
        LocalDate hoy = LocalDate.now(); 
        
        if (codigo != null && !codigo.trim().isEmpty()) {
            String codigoLimpio = codigo.trim();
            
            if (codigoLimpio.startsWith("CT")) {
                citaRepo.findByNumeroCita(codigoLimpio)
                        .filter(cita -> cita.getFechaHoraCita().toLocalDate().equals(hoy))
                        .filter(cita -> !cita.getEstado().name().equals("ATENDIDA")) 
                        .ifPresent(cita -> {
                            cita.setEstado(com.clinica.model.entities.Cita.EstadoCita.ATENDIDA);
                            atencionBuilder.cita(cita);
                        });
                        
            } else if (codigoLimpio.startsWith("OE")) {
                ordenAtencionEmergenciaRepo.findByNumeroOrden(codigoLimpio)
                        .filter(orden -> orden.getCreatedAt().toLocalDate().equals(hoy))
                        .filter(orden -> !orden.getEstado().name().equals("FINALIZADO"))
                        .ifPresent(orden -> {
                            orden.setEstado(com.clinica.model.entities.OrdenAtencionEmergencia.EstadoOrden.FINALIZADO);
                            atencionBuilder.ordenEmergencia(orden);
                        });
            }
        }

        AtencionMedica nuevaAtencion = atencionBuilder.build();
        AtencionMedica atencionGuardada = atencionRepo.save(nuevaAtencion);

        return atencionGuardada.getId();
    }

    @Transactional(readOnly = true)
    public String verificarEstadoCitaUOrden(String codigoAtencion) {
        if (codigoAtencion == null || codigoAtencion.trim().isEmpty()) {
            return "NO_EXISTE";
        }

        String codigoLimpio = codigoAtencion.trim();
        LocalDate hoy = LocalDate.now();

        if (codigoLimpio.startsWith("CT")) {
            return citaRepo.findByNumeroCita(codigoLimpio)
                    .map(cita -> {
                        if (!cita.getFechaHoraCita().toLocalDate().equals(hoy)) return "OTRA_FECHA";
                        if (cita.getEstado().name().equals("ATENDIDA")) return "ATENDIDA";
                        return "VALIDA";
                    })
                    .orElse("NO_EXISTE");
                    
        } else if (codigoLimpio.startsWith("OE")) {
            return ordenAtencionEmergenciaRepo.findByNumeroOrden(codigoLimpio)
                    .map(orden -> {
                        if (!orden.getCreatedAt().toLocalDate().equals(hoy)) return "OTRA_FECHA";
                        if (orden.getEstado().name().equals("FINALIZADO")) return "ATENDIDA";
                        return "VALIDA";
                    })
                    .orElse("NO_EXISTE");
        }
        
        return "NO_EXISTE";
    }
}