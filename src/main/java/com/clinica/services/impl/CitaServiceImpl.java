package com.clinica.services.impl;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.model.entities.Cita;
import com.clinica.model.repositories.*;
import com.clinica.services.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

        private final CitaRepository citaRepository;
        private final PacienteRepository pacienteRepository;
        private final TrabajadorRepository trabajadorRepository;
        private final ConsultorioRepository consultorioRepository;
        private final TipoCitaRepository tipoCitaRepository;

        @Override
        @Transactional
        public CitaResponseDTO programarCita(CitaRequestDTO request) {

                // 1. Obtener entidades relacionadas
                var paciente = pacienteRepository.findByHistoriaClinicaId(request.getHistoriaClinicaId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Paciente no encontrado para la Historia Clínica con ID: "
                                                                + request.getHistoriaClinicaId()));

                var medico = trabajadorRepository.findById(request.getMedicoId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Médico no encontrado con ID: " + request.getMedicoId()));

                var consultorio = consultorioRepository.findById(request.getConsultorioId())
                                .orElseThrow(
                                                () -> new RuntimeException("Consultorio no encontrado con ID: "
                                                                + request.getConsultorioId()));

                var tipoCita = tipoCitaRepository.findById(request.getTipoCitaId())
                                .orElseThrow(
                                                () -> new RuntimeException("Tipo de Cita no encontrado con ID: "
                                                                + request.getTipoCitaId()));

                var creador = trabajadorRepository.findById(request.getCreadoPorId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Usuario creador no encontrado con ID: " + request.getCreadoPorId()));

                // 2. Generar número de cita único
                String fechaStr = request.getFechaHora().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String numeroCita = "CT-" + fechaStr + "-" + (System.currentTimeMillis() % 10000);

                // 3. Construir y guardar la entidad
                Cita cita = Cita.builder()
                                .numeroCita(numeroCita)
                                .paciente(paciente)
                                .medico(medico)
                                .consultorio(consultorio)
                                .tipoCita(tipoCita)
                                .fechaHoraCita(request.getFechaHora())
                                .motivoConsulta(request.getMotivoConsulta())
                                .creadoPor(creador)
                                .estado(Cita.EstadoCita.PROGRAMADA)
                                .build();

                Cita guardada = citaRepository.save(cita);

                return mapToResponseDTO(guardada);
        }

        @Override
        public List<CitaResponseDTO> listarCitas() {
                return citaRepository.findAll().stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        private CitaResponseDTO mapToResponseDTO(Cita cita) {
                return CitaResponseDTO.builder()
                                .id(cita.getId())
                                .numeroCita(cita.getNumeroCita())
                                .nombrePaciente(cita.getPaciente().getNombreCompleto())
                                .nombreMedico(cita.getMedico().getNombreCompleto())
                                .consultorio(cita.getConsultorio().getNombre())
                                .fechaHoraCita(cita.getFechaHoraCita())
                                .estado(cita.getEstado().name())
                                .build();
        }
}