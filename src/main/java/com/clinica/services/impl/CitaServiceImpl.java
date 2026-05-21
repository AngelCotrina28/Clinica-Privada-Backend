package com.clinica.services.impl;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.model.entities.*;
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
        private final TurnoRepository turnoRepository;

        @Override
        @Transactional
        public CitaResponseDTO programarCita(CitaRequestDTO request) {

                // 1. Buscamos directamente al paciente, sin intermediarios
                var paciente = pacienteRepository.findById(request.getPacienteId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Paciente no encontrado con ID: " + request.getPacienteId()));

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

                String fechaStr = request.getFechaHora().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String numeroCita = "CT-" + fechaStr + "-" + (System.currentTimeMillis() % 10000);

                // 2. Construimos la cita vinculándola limpiamente al paciente
                Cita cita = Cita.builder()
                                .numeroCita(numeroCita)
                                .paciente(paciente) // Vinculación directa y limpia
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
                                .nombrePaciente(cita.getPaciente().getNombreCompleto()) // Extraemos el nombre directamente del paciente
                                .nombreMedico(cita.getMedico().getNombreCompleto())
                                .consultorio(cita.getConsultorio().getNombre())
                                .fechaHoraCita(cita.getFechaHoraCita())
                                .estado(cita.getEstado().name())
                                .build();
        }

        @Override
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public List<com.clinica.dtos.DisponibilidadResponseDTO> consultarDisponibilidad(Long medicoId,
                        java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin) {

                // 1. Obtener todos los turnos configurados para el médico
                var turnos = turnoRepository.findByMedicoIdAndActivoTrue(medicoId);

                // 2. Obtener todas las citas ya agendadas y activas en ese rango de fechas
                var citasExistentes = citaRepository.findByMedicoIdAndFechaHoraCitaBetweenAndEstadoNot(
                                medicoId,
                                fechaInicio.atStartOfDay(),
                                fechaFin.plusDays(1).atStartOfDay(),
                                Cita.EstadoCita.CANCELADA);

                java.util.List<com.clinica.dtos.DisponibilidadResponseDTO> agendaCompleta = new java.util.ArrayList<>();

                // 3. Iterar día por día en el rango solicitado por el frontend
                for (java.time.LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
                        final java.time.LocalDate fechaActual = fecha;

                        // CORRECCIÓN 1: Uso moderno de Locale (Java 19+) para evitar deprecación
                        String nombreDia = fechaActual.getDayOfWeek().getDisplayName(
                                        java.time.format.TextStyle.FULL,
                                        java.util.Locale.forLanguageTag("es-PE"));

                        // CORRECCIÓN 2: Se eliminó la variable 'diaSemanaEnum' que no se utilizaba
                        // ("Dead code")

                        var turnoDelDia = turnos.stream()
                                        .filter(t -> t.getDiaSemana().name()
                                                        .equals(mapToDiaSemanaEnum(fechaActual.getDayOfWeek())))
                                        .findFirst();

                        java.util.List<String> horasCalculadas = new java.util.ArrayList<>();

                        if (turnoDelDia.isPresent()) {
                                var turno = turnoDelDia.get();
                                java.time.LocalTime tiempoActual = turno.getHoraInicio();

                                // Generar los bloques teóricos sumando la duración en minutos
                                while (tiempoActual.plusMinutes(turno.getDuracionMinutos()).isBefore(turno.getHoraFin())
                                                ||
                                                tiempoActual.plusMinutes(turno.getDuracionMinutos())
                                                                .equals(turno.getHoraFin())) {

                                        java.time.LocalDateTime bloqueEvaluado = java.time.LocalDateTime.of(fechaActual,
                                                        tiempoActual);

                                        boolean estaOcupado = citasExistentes.stream()
                                                        .anyMatch(c -> c.getFechaHoraCita().equals(bloqueEvaluado));

                                        if (!estaOcupado) {
                                                horasCalculadas.add(tiempoActual.format(
                                                                java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                                        }

                                        tiempoActual = tiempoActual.plusMinutes(turno.getDuracionMinutos());
                                }
                        }

                        agendaCompleta.add(com.clinica.dtos.DisponibilidadResponseDTO.builder()
                                        .fecha(fechaActual)
                                        .diaNombre(nombreDia.substring(0, 1).toUpperCase() + nombreDia.substring(1))
                                        .horariosDisponibles(horasCalculadas)
                                        .build());
                }

                return agendaCompleta;
        }

        private String mapToDiaSemanaEnum(java.time.DayOfWeek day) {
                switch (day) {
                        case MONDAY:
                                return "LUNES";
                        case TUESDAY:
                                return "MARTES";
                        case WEDNESDAY:
                                return "MIERCOLES";
                        case THURSDAY:
                                return "JUEVES";
                        case FRIDAY:
                                return "VIERNES";
                        case SATURDAY:
                                return "SABADO";
                        default:
                                return "DOMINGO";
                }
        }
}