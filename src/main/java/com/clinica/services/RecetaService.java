package com.clinica.services;

import com.clinica.dtos.DetalleRecetaRequestDTO;
import com.clinica.dtos.DetalleRecetaResponseDTO;
import com.clinica.dtos.RecetaRequestDTO;
import com.clinica.dtos.RecetaResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.*;
import com.clinica.model.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaService {

    private static final String PREFIJO_RECETA = "REC-";
    private static final int LONGITUD_CORRELATIVO = 6;

    private final RecetaRepository recetaRepo;
    private final MedicamentoRepository medicamentoRepo;
    private final AtencionMedicaRepository atencionMedicaRepo;
    private final TrabajadorRepository trabajadorRepo;
    private final PacienteRepository pacienteRepo;

    // ------------------------------------------------------------------
    // BUSQUEDA
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RecetaResponseDTO> buscar(String termino) {
        if (termino == null || termino.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar un número de receta o el DNI del paciente.");
        }

        List<Receta> recetas = recetaRepo.buscarPorNumeroRecetaODni(termino.trim());

        if (recetas.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se encontró ninguna receta para el criterio: " + termino);
        }

        return recetas.stream().map(this::toResponseDTO).toList();
    }

    // REGISTRO (generación automática de numeroReceta)
    @Transactional
    public RecetaResponseDTO registrar(RecetaRequestDTO dto) {
        AtencionMedica atencion = atencionMedicaRepo.findById(dto.getAtencionMedicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Atención médica no encontrada: " + dto.getAtencionMedicaId()));

        Trabajador medico = trabajadorRepo.findById(dto.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Médico no encontrado: " + dto.getMedicoId()));

        Paciente paciente = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado: " + dto.getPacienteId()));

        Receta receta = Receta.builder()
                .numeroReceta(generarNumeroReceta())
                .atencionMedica(atencion)
                .medico(medico)
                .paciente(paciente)
                .indicacionesGenerales(dto.getIndicacionesGenerales())
                .fechaVencimiento(dto.getFechaVencimiento())
                .estado(Receta.EstadoReceta.EMITIDA)
                .build();

        List<DetalleReceta> detalles = new ArrayList<>();
        for (DetalleRecetaRequestDTO d : dto.getDetalles()) {
            Medicamento medicamento = medicamentoRepo.findById(d.getMedicamentoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Medicamento no encontrado: " + d.getMedicamentoId()));

            detalles.add(DetalleReceta.builder()
                    .receta(receta)
                    .medicamento(medicamento)
                    .dosis(d.getDosis())
                    .frecuencia(d.getFrecuencia())
                    .duracion(d.getDuracion())
                    .cantidadPrescrita(d.getCantidadPrescrita())
                    .cantidadDespachada(0)
                    .viaAdministracion(d.getViaAdministracion())
                    .indicaciones(d.getIndicaciones())
                    .build());
        }
        receta.setDetalles(detalles);

        receta = recetaRepo.save(receta);
        log.info("Receta registrada: {} para paciente {}", receta.getNumeroReceta(), paciente.getDni());

        return toResponseDTO(receta);
    }

    private String generarNumeroReceta() {
        Optional<Receta> ultimaReceta = recetaRepo.findFirstByOrderByIdDesc();

        int siguienteCorrelativo = 1;
        if (ultimaReceta.isPresent()) {
            String soloDigitos = ultimaReceta.get().getNumeroReceta().replaceAll("\\D+", "");
            if (!soloDigitos.isBlank()) {
                siguienteCorrelativo = Integer.parseInt(soloDigitos) + 1;
            }
        }

        return PREFIJO_RECETA + String.format("%0" + LONGITUD_CORRELATIVO + "d", siguienteCorrelativo);
    }

    @Transactional
    public RecetaResponseDTO despachar(Long recetaId) {
        Receta receta = obtenerEntidad(recetaId);

        if (receta.getEstado() != Receta.EstadoReceta.EMITIDA) {
            throw new IllegalStateException(
                    "Receta ya despachada: solo se pueden despachar recetas en estado EMITIDA. Estado actual: "
                            + receta.getEstado());
        }

        List<DetalleReceta> detalles = receta.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalStateException("La receta no tiene medicamentos asociados para despachar.");
        }

        for (DetalleReceta detalle : detalles) {
            Medicamento medicamento = detalle.getMedicamento();
            int cantidadADescontar = detalle.getCantidadPrescrita();

            if (medicamento.getStockActual() < cantidadADescontar) {
                throw new IllegalStateException(
                        "Stock insuficiente para el medicamento '" + medicamento.getNombre()
                                + "'. Stock actual: " + medicamento.getStockActual()
                                + ", cantidad requerida: " + cantidadADescontar);
            }

            medicamento.setStockActual(medicamento.getStockActual() - cantidadADescontar);
            detalle.setCantidadDespachada(cantidadADescontar);
            medicamentoRepo.save(medicamento);
        }

        receta.setEstado(Receta.EstadoReceta.DESPACHADA);
        receta = recetaRepo.save(receta);

        log.info("Receta despachada: {}", receta.getNumeroReceta());
        return toResponseDTO(receta);
    }
    
    private Receta obtenerEntidad(Long id) {
        return recetaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada: " + id));
    }

    private RecetaResponseDTO toResponseDTO(Receta receta) {
        List<DetalleRecetaResponseDTO> detalles = receta.getDetalles() == null
                ? List.of()
                : receta.getDetalles().stream()
                    .map(d -> DetalleRecetaResponseDTO.builder()
                            .id(d.getId())
                            .medicamentoId(d.getMedicamento().getId())
                            .medicamentoNombre(d.getMedicamento().getNombre())
                            .presentacion(d.getMedicamento().getPresentacion())
                            .dosis(d.getDosis())
                            .frecuencia(d.getFrecuencia())
                            .duracion(d.getDuracion())
                            .cantidadPrescrita(d.getCantidadPrescrita())
                            .cantidadDespachada(d.getCantidadDespachada())
                            .viaAdministracion(d.getViaAdministracion())
                            .indicaciones(d.getIndicaciones())
                            .build())
                    .toList();

        return RecetaResponseDTO.builder()
                .id(receta.getId())
                .numeroReceta(receta.getNumeroReceta())
                .pacienteNombre(receta.getPaciente().getNombreCompleto())
                .pacienteDni(receta.getPaciente().getDni())
                .medicoNombre(receta.getMedico().getNombreCompleto())
                .indicacionesGenerales(receta.getIndicacionesGenerales())
                .estado(receta.getEstado().name())
                .fechaEmision(receta.getFechaEmision())
                .fechaVencimiento(receta.getFechaVencimiento())
                .detalles(detalles)
                .build();
    }
}