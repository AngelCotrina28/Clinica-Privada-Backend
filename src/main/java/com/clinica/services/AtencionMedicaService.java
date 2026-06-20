package com.clinica.services;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.dtos.AtencionMedicaRequestDTO;
import com.clinica.dtos.CitaOpcionDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.DetalleReceta; 
import com.clinica.model.entities.HistoriaClinica;
import com.clinica.model.entities.Medicamento; 
import com.clinica.model.entities.OrdenAtencionEmergencia;
import com.clinica.model.entities.Receta; 
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.AtencionMedicaRepository;
import com.clinica.model.repositories.CitaRepository;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.MedicamentoRepository; 
import com.clinica.model.repositories.OrdenAtencionEmergenciaRepository;
import com.clinica.model.repositories.RecetaRepository; 
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtencionMedicaService {

    private final AtencionMedicaRepository atencionRepo;
    private final HistoriaClinicaRepository historiaRepo;
    private final TrabajadorRepository trabajadorRepo;
    private final CitaRepository citaRepo;
    private final OrdenAtencionEmergenciaRepository ordenAtencionEmergenciaRepo;
    
    // Repositorios necesarios para crear recetas
    private final MedicamentoRepository medicamentoRepo;
    private final RecetaRepository recetaRepo;

    @Transactional(readOnly = true)
    public List<AtencionMedicaHistorialDTO> obtenerHistorialPorPaciente(Long historiaId) {
        return atencionRepo.findByHistoriaClinicaIdOrderByFechaHoraInicioDesc(historiaId)
                .stream()
                .map(this::mapToHistorialDTO)
                .toList();
    }

    @Transactional
    public Long registrarAtencion(AtencionMedicaRequestDTO request) {
        HistoriaClinica historia = historiaRepo.findById(request.getHistoriaClinicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clinica no encontrada con ID: " + request.getHistoriaClinicaId()));

        Trabajador medico = trabajadorRepo.findById(request.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Medico no encontrado con ID: " + request.getMedicoId()));
        validarMedicoActivo(medico);

        AtencionMedica.AtencionMedicaBuilder atencionBuilder = AtencionMedica.builder()
                .historiaClinica(historia)
                .medico(medico)
                .diagnosticoPrincipal(request.getDiagnosticoPrincipal())
                .observaciones(request.getNotasEvolucion());

        if (request.getNumeroCita() != null && !request.getNumeroCita().isBlank()) {
            vincularCodigoAtencion(request.getNumeroCita().trim(), historia, medico, atencionBuilder);
        }

        // 1. Guardamos la atención y capturamos la entidad persistida
        AtencionMedica atencionGuardada = atencionRepo.save(atencionBuilder.build());

        // 2. Procesamos y guardamos la receta digital si se adjuntó
        if (request.getItemsReceta() != null && !request.getItemsReceta().isEmpty()) {

            Receta receta = Receta.builder()
                    .numeroReceta(generarNumeroRecetaUnico())
                    .atencionMedica(atencionGuardada)
                    .medico(medico)
                    .paciente(historia.getPaciente()) // ¡Solo hacemos esto! Es 100% seguro.
                    .estado(Receta.EstadoReceta.EMITIDA)
                    .fechaVencimiento(LocalDate.now().plusMonths(1))
                    .build();

            for (var itemDto : request.getItemsReceta()) {
                Medicamento med = medicamentoRepo.findById(itemDto.getMedicamentoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Medicamento no encontrado con ID: " + itemDto.getMedicamentoId()));

                DetalleReceta detalle = DetalleReceta.builder()
                        .receta(receta)
                        .medicamento(med)
                        .cantidadPrescrita(itemDto.getCantidad())
                        .duracion(itemDto.getDias() + " días")
                        .indicaciones(itemDto.getIndicaciones())
                        .cantidadDespachada(0)
                        .build();

                receta.getDetalles().add(detalle);
            }

            recetaRepo.save(receta);
        }

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
                    .map(cita -> estadoCita(cita, hoy))
                    .orElse("NO_EXISTE");
        }

        if (codigoLimpio.startsWith("OE")) {
            return ordenAtencionEmergenciaRepo.findByNumeroOrden(codigoLimpio)
                    .map(orden -> estadoOrden(orden, hoy))
                    .orElse("NO_EXISTE");
        }

        return "NO_EXISTE";
    }

    @Transactional(readOnly = true)
    public List<CitaOpcionDTO> obtenerCitasDisponibles(Long historiaId) {
        List<CitaOpcionDTO> resultado = new ArrayList<>();

        citaRepo.findByHistoriaClinicaIdAndEstado(historiaId, Cita.EstadoCita.CONFIRMADA)
                .forEach(c -> resultado.add(CitaOpcionDTO.builder()
                        .codigo(c.getNumeroCita())
                        .tipo("CITA")
                        .fecha(c.getFechaHoraCita())
                        .build()));

        ordenAtencionEmergenciaRepo
                .findByHistoriaClinicaIdAndEstado(historiaId, OrdenAtencionEmergencia.EstadoOrden.PENDIENTE)
                .forEach(o -> resultado.add(CitaOpcionDTO.builder()
                        .codigo(o.getNumeroOrden())
                        .tipo("EMERGENCIA")
                        .fecha(o.getCreatedAt())
                        .build()));

        return resultado;
    }

    private void vincularCodigoAtencion(
            String codigo,
            HistoriaClinica historia,
            Trabajador medico,
            AtencionMedica.AtencionMedicaBuilder atencionBuilder) {
        if (codigo.startsWith("CT")) {
            Cita cita = citaRepo.findByNumeroCita(codigo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + codigo));
            validarCitaAtendible(cita, historia, medico);
            cita.setEstado(Cita.EstadoCita.ATENDIDA);
            atencionBuilder.cita(cita);
            atencionBuilder.motivoConsulta(cita.getMotivoConsulta());
            return;
        }

        if (codigo.startsWith("OE")) {
            OrdenAtencionEmergencia orden = ordenAtencionEmergenciaRepo.findByNumeroOrden(codigo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Orden de emergencia no encontrada: " + codigo));
            validarOrdenAtendible(orden, historia, medico);
            orden.setEstado(OrdenAtencionEmergencia.EstadoOrden.FINALIZADO);
            atencionBuilder.ordenEmergencia(orden);
            atencionBuilder.motivoConsulta(orden.getMotivo());
            return;
        }

        throw new IllegalArgumentException("Codigo de atencion no reconocido.");
    }

    private void validarCitaAtendible(Cita cita, HistoriaClinica historia, Trabajador medico) {
        if (!cita.getFechaHoraCita().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalStateException("La cita no corresponde a la fecha actual.");
        }
        if (!cita.getHistoriaClinica().getId().equals(historia.getId())) {
            throw new IllegalArgumentException("La cita no pertenece a la historia clinica indicada.");
        }
        if (!cita.getMedico().getId().equals(medico.getId())) {
            throw new IllegalArgumentException("La cita no pertenece al medico indicado.");
        }
        if (cita.getEstado() != Cita.EstadoCita.CONFIRMADA) {
            throw new IllegalStateException("La cita no esta disponible para atencion.");
        }
    }

    private void validarOrdenAtendible(OrdenAtencionEmergencia orden, HistoriaClinica historia, Trabajador medico) {
        if (!orden.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalStateException("La orden no corresponde a la fecha actual.");
        }
        if (!orden.getHistoriaClinica().getId().equals(historia.getId())) {
            throw new IllegalArgumentException("La orden no belongs a la historia clinica indicada.");
        }
        if (!orden.getMedico().getId().equals(medico.getId())) {
            throw new IllegalArgumentException("La orden no pertenece al medico indicado.");
        }
        if (orden.getEstado() != OrdenAtencionEmergencia.EstadoOrden.PENDIENTE) {
            throw new IllegalStateException("La orden no esta disponible para atencion.");
        }
    }

    private void validarMedicoActivo(Trabajador medico) {
        if (medico.getRol() == null || !"MEDICO".equalsIgnoreCase(medico.getRol().getNombre())) {
            throw new IllegalArgumentException("El trabajador indicado no tiene rol MEDICO.");
        }
        if (!medico.isActivo()) {
            throw new IllegalStateException("El medico indicado no esta activo.");
        }
    }

    private String estadoCita(Cita cita, LocalDate hoy) {
        if (!cita.getFechaHoraCita().toLocalDate().equals(hoy)) {
            return "OTRA_FECHA";
        }
        if (cita.getEstado() == Cita.EstadoCita.ATENDIDA) {
            return "ATENDIDA";
        }
        if (cita.getEstado() == Cita.EstadoCita.CONFIRMADA) {
            return "VALIDA";
        }
        return "NO_DISPONIBLE";
    }

    private String estadoOrden(OrdenAtencionEmergencia orden, LocalDate hoy) {
        if (!orden.getCreatedAt().toLocalDate().equals(hoy)) {
            return "OTRA_FECHA";
        }
        if (orden.getEstado() == OrdenAtencionEmergencia.EstadoOrden.FINALIZADO) {
            return "ATENDIDA";
        }
        if (orden.getEstado() == OrdenAtencionEmergencia.EstadoOrden.PENDIENTE) {
            return "VALIDA";
        }
        return "NO_DISPONIBLE";
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

    private String generarNumeroRecetaUnico() {
        String fechaParte = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int aleatorio = (int) (10000 + Math.random() * 90000);
        String codigo = "REC-" + fechaParte + "-" + aleatorio;

        int intentos = 0;
        
        while (recetaRepo.existsByNumeroReceta(codigo) && intentos < 5) {
            aleatorio = (int) (10000 + Math.random() * 90000);
            codigo = "REC-" + fechaParte + "-" + aleatorio;
            intentos++;
        }
        return codigo;
    }
}