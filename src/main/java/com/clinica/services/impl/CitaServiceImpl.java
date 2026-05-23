package com.clinica.services.impl;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.HorarioBloqueDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.*;
import com.clinica.model.repositories.*;
import com.clinica.services.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private static final List<Cita.EstadoCita> ESTADOS_QUE_BLOQUEAN = List.of(
            Cita.EstadoCita.PROGRAMADA,
            Cita.EstadoCita.CONFIRMADA);

    private final CitaRepository citaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final TipoCitaRepository tipoCitaRepository;
    private final TurnoRepository turnoRepository;

    @Override
    @Transactional
    public CitaResponseDTO programarCita(CitaRequestDTO request) {
        if (request.getTurnoId() == null) {
            Turno.DiaSemana diaSemana = convertirDiaSemana(request.getFechaHora().getDayOfWeek());
            List<Turno> turnos = turnoRepository.findTurnosActivosDelMedico(
                    request.getMedicoId(), request.getFechaHora().toLocalDate(), request.getFechaHora().toLocalDate().minusDays(1), diaSemana);
            
            LocalTime horaSolicitada = request.getFechaHora().toLocalTime();
            
            // Buscamos el turno específico que contenga la hora solicitada
            Turno turnoCorrecto = turnos.stream()
                .filter(t -> !horaSolicitada.isBefore(t.getHoraInicio()) && horaSolicitada.isBefore(t.getHoraFin()))
                .findFirst()
                .orElse(!turnos.isEmpty() ? turnos.get(0) : null);

            if (turnoCorrecto != null) {
                request.setTurnoId(turnoCorrecto.getId());
                // Si tampoco nos enviaron consultorio, lo tomamos de este turno exacto
                if (request.getConsultorioId() == null) {
                    request.setConsultorioId(turnoCorrecto.getConsultorio().getId());
                }
            }
        }

        validarRequest(request);

        Trabajador creador = obtenerTrabajadorAutenticado(request.getCreadoPorId());

        // 1. OBTENER LA HISTORIA CLÍNICA (NUEVO)
        HistoriaClinica historia = historiaClinicaRepository.findById(request.getHistoriaClinicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clinica no encontrada con ID: " + request.getHistoriaClinicaId()));

        // 2. Pasamos la entidad 'historia' directamente en lugar de solo el ID (requiere el ajuste del paso 2)
        Paciente paciente = obtenerOCrearPacienteDesdeHistoria(historia, creador);

        Trabajador medico = trabajadorRepository.findById(request.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Medico no encontrado con ID: " + request.getMedicoId()));

        if (!medico.isActivo()) {
            throw new IllegalStateException("El medico seleccionado no esta activo.");
        }
        if (!esMedico(medico)) {
            throw new IllegalArgumentException("El trabajador seleccionado no tiene rol MEDICO.");
        }

        Turno turno = turnoRepository.findById(request.getTurnoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Turno no encontrado con ID: " + request.getTurnoId()));

        validarTurno(request, medico, turno);

        if (citaRepository.existsByMedicoIdAndFechaHoraCitaAndEstadoIn(
                medico.getId(), request.getFechaHora(), ESTADOS_QUE_BLOQUEAN)) {
            throw new IllegalStateException("El bloque horario seleccionado ya esta ocupado.");
        }

        TipoCita tipoCita = obtenerTipoConsultaExterna();

        Cita cita = Cita.builder()
                .numeroCita(generarNumeroCita(request.getFechaHora()))
                .historiaClinica(historia) // <--- LÍNEA CRÍTICA AÑADIDA
                .paciente(paciente)
                .medico(medico)
                .consultorio(turno.getConsultorio())
                .turno(turno)
                .tipoCita(tipoCita)
                .fechaHoraCita(request.getFechaHora())
                .motivoConsulta(request.getMotivoConsulta())
                .creadoPor(creador)
                .estado(Cita.EstadoCita.CONFIRMADA)
                .build();

        return mapToResponseDTO(citaRepository.save(cita));
    }

    // ── MOTOR MENSUAL ──
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.clinica.dtos.DisponibilidadResponseDTO> consultarDisponibilidad(Long medicoId,
            LocalDate fechaInicio, LocalDate fechaFin) {

        var turnos = turnoRepository.findByMedicoIdAndActivoTrue(medicoId);
        var citasExistentes = citaRepository.findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(
                medicoId, fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay(), ESTADOS_QUE_BLOQUEAN);

        java.util.List<com.clinica.dtos.DisponibilidadResponseDTO> agendaCompleta = new java.util.ArrayList<>();

        for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
            final LocalDate fechaActual = fecha;
            String nombreDia = fechaActual.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es-PE"));

            var turnoDelDia = turnos.stream()
                    .filter(t -> t.getDiaSemana() == convertirDiaSemana(fechaActual.getDayOfWeek()))
                    .findFirst();

            java.util.List<String> horasCalculadas = new java.util.ArrayList<>();

            if (turnoDelDia.isPresent()) {
                var turno = turnoDelDia.get();
                LocalTime tiempoActual = turno.getHoraInicio();

                while (tiempoActual.plusMinutes(turno.getDuracionMinutos()).isBefore(turno.getHoraFin())
                        || tiempoActual.plusMinutes(turno.getDuracionMinutos()).equals(turno.getHoraFin())) {

                    LocalDateTime bloqueEvaluado = LocalDateTime.of(fechaActual, tiempoActual);
                    boolean estaOcupado = citasExistentes.stream()
                            .anyMatch(c -> c.getFechaHoraCita().equals(bloqueEvaluado));

                    if (!estaOcupado) {
                        horasCalculadas.add(tiempoActual.format(DateTimeFormatter.ofPattern("HH:mm")));
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

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponseDTO> listarCitas() {
        return citaRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorResponseDTO> listarMedicosPorEspecialidad(Long especialidadId) {
        if (especialidadId == null) throw new IllegalArgumentException("Debe seleccionar especialidad.");
        return turnoRepository.findMedicosActivosByEspecialidad(especialidadId).stream()
                .map(this::mapTrabajadorToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioBloqueDTO> obtenerDisponibilidad(Long medicoId, LocalDate fecha) {
        if (medicoId == null || fecha == null) throw new IllegalArgumentException("Seleccione medico y fecha.");

        Turno.DiaSemana diaSemana = convertirDiaSemana(fecha.getDayOfWeek());
        List<Turno> turnos = turnoRepository.findTurnosActivosDelMedico(medicoId, fecha, fecha.minusDays(1), diaSemana);
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        Map<LocalDateTime, Cita> citasOcupadas = citaRepository
                .findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(medicoId, inicioDia, finDia, ESTADOS_QUE_BLOQUEAN)
                .stream().collect(Collectors.toMap(Cita::getFechaHoraCita, Function.identity(), (a, b) -> a));

        return turnos.stream()
                .flatMap(turno -> generarBloques(fecha, turno, citasOcupadas).stream())
                .collect(Collectors.toList());
    }

    private List<HorarioBloqueDTO> generarBloques(LocalDate fecha, Turno turno, Map<LocalDateTime, Cita> citasOcupadas) {
        java.util.ArrayList<HorarioBloqueDTO> bloques = new java.util.ArrayList<>();
        LocalDateTime inicioTurno = inicioTurno(turno, fecha);
        LocalDateTime finTurno = finTurno(turno, fecha);
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        LocalDateTime cursor = inicioTurno.isBefore(inicioDia) ? inicioDia : inicioTurno;

        while (!cursor.plusMinutes(turno.getDuracionMinutos()).isAfter(finTurno) && cursor.isBefore(finDia)) {
            LocalDateTime fechaHora = cursor;
            Cita citaOcupada = citasOcupadas.get(fechaHora);
            boolean disponible = citaOcupada == null && !fechaHora.isBefore(LocalDateTime.now());

            bloques.add(HorarioBloqueDTO.builder()
                    .turnoId(turno.getId())
                    .consultorioId(turno.getConsultorio().getId())
                    .consultorio(turno.getConsultorio().getNombre())
                    .horaInicio(fechaHora.toLocalTime())
                    .horaFin(fechaHora.plusMinutes(turno.getDuracionMinutos()).toLocalTime())
                    .disponible(disponible)
                    .estado(disponible ? "DISPONIBLE" : "OCUPADO")
                    .citaId(citaOcupada != null ? citaOcupada.getId() : null)
                    .numeroCita(citaOcupada != null ? citaOcupada.getNumeroCita() : null)
                    .build());
            cursor = cursor.plusMinutes(turno.getDuracionMinutos());
        }
        return bloques;
    }

    
    private Paciente obtenerOCrearPacienteDesdeHistoria(HistoriaClinica historia, Trabajador registrador) {
        return pacienteRepository.findByHistoriaClinicaId(historia.getId())
                .orElseGet(() -> pacienteRepository.findByDni(historia.getDniPaciente())
                        .map(paciente -> vincularPacienteConHistoria(paciente, historia))
                        .orElseGet(() -> crearPacienteDesdeHistoria(historia, registrador))
                );
    }

    private Paciente vincularPacienteConHistoria(Paciente paciente, HistoriaClinica historia) {
        if (!paciente.isActivo()) throw new IllegalStateException("Paciente inactivo.");
        paciente.setHistoriaClinicaId(historia.getId());
        sincronizarDatosBasicosPaciente(paciente, historia);
        return pacienteRepository.save(paciente);
    }

    private Paciente crearPacienteDesdeHistoria(HistoriaClinica historia, Trabajador registrador) {
        Paciente paciente = Paciente.builder()
                .dni(historia.getDniPaciente())
                .nombreCompleto(historia.getNombreCompleto())
                .fechaNacimiento(parseFechaNacimiento(historia.getFechaNacimiento()))
                .genero(valorOpcional(historia.getGenero()))
                .telefono(valorOpcional(historia.getTelefono()))
                .email(valorOpcional(historia.getEmail()))
                .direccion(valorOpcional(historia.getDireccion()))
                .historiaClinicaId(historia.getId())
                .registradoPor(registrador)
                .activo(true).build();
        return pacienteRepository.save(paciente);
    }

    private void sincronizarDatosBasicosPaciente(Paciente paciente, HistoriaClinica historia) {
        paciente.setNombreCompleto(historia.getNombreCompleto());
        paciente.setFechaNacimiento(parseFechaNacimiento(historia.getFechaNacimiento()));
        paciente.setGenero(valorOpcional(historia.getGenero()));
        paciente.setTelefono(valorOpcional(historia.getTelefono()));
        paciente.setEmail(valorOpcional(historia.getEmail()));
        paciente.setDireccion(valorOpcional(historia.getDireccion()));
    }

    private LocalDate parseFechaNacimiento(String fechaNacimiento) {
        if (fechaNacimiento == null || fechaNacimiento.isBlank()) return null;
        try { return LocalDate.parse(fechaNacimiento.trim()); } catch (DateTimeParseException ex) { return null; }
    }

    private String valorOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validarRequest(CitaRequestDTO request) {
        if (request.getHistoriaClinicaId() == null && request.getPacienteId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una historia clinica o paciente.");
        }
        if (request.getMedicoId() == null) throw new IllegalArgumentException("Debe seleccionar un medico.");
        if (request.getTurnoId() == null) throw new IllegalArgumentException("Debe seleccionar un bloque horario.");
        if (request.getFechaHora() == null) throw new IllegalArgumentException("Debe seleccionar fecha y hora.");
    }

    private void validarTurno(CitaRequestDTO request, Trabajador medico, Turno turno) {
        if (!esMedico(turno.getMedico())) throw new IllegalArgumentException("Turno no es de medico.");
        if (!turno.isActivo()) throw new IllegalStateException("Turno inactivo.");
        if (!turno.getMedico().getId().equals(medico.getId())) throw new IllegalArgumentException("Turno no pertenece al medico.");
        
        if (request.getEspecialidadId() != null) {
            boolean pertenece = medico.getEspecialidades().stream().anyMatch(e -> e.getId().equals(request.getEspecialidadId()));
            if (!pertenece) throw new IllegalArgumentException("Medico no pertenece a especialidad.");
        }

        LocalDate fecha = request.getFechaHora().toLocalDate();
        LocalDateTime inicioTurno = inicioTurno(turno, fecha);
        LocalDateTime finTurno = finTurno(turno, fecha);

        if (turno.getFecha() != null && !turno.getFecha().equals(fecha) && !(esNocturno(turno) && turno.getFecha().plusDays(1).equals(fecha))) {
            throw new IllegalArgumentException("Turno no corresponde a la fecha.");
        }
        if (!esNocturno(turno) && turno.getDiaSemana() != convertirDiaSemana(fecha.getDayOfWeek())) {
            throw new IllegalArgumentException("Turno no corresponde al dia.");
        }
        if (request.getFechaHora().isBefore(inicioTurno) || request.getFechaHora().plusMinutes(turno.getDuracionMinutos()).isAfter(finTurno)) {
            throw new IllegalArgumentException("Hora fuera de turno.");
        }

        long minutosTranscurridos = java.time.Duration.between(inicioTurno, request.getFechaHora()).toMinutes();
        if (minutosTranscurridos % turno.getDuracionMinutos() != 0) {
            throw new IllegalArgumentException("La hora debe coincidir con los bloques de " + turno.getDuracionMinutos() + " minutos configurados para este turno.");
        }
    }

    private TipoCita obtenerTipoConsultaExterna() {
        return tipoCitaRepository.findFirstByNombreIgnoreCaseAndActivoTrue("CONSULTA EXTERNA")
                .or(() -> tipoCitaRepository.findFirstByActivoTrueOrderByIdAsc())
                .orElseGet(() -> tipoCitaRepository.save(TipoCita.builder()
                        .nombre("CONSULTA EXTERNA").descripcion("Cita externa").duracionMinutos(20).activo(true).build()));
    }

    private LocalDateTime inicioTurno(Turno turno, LocalDate fechaConsulta) {
        return LocalDateTime.of(turno.getFecha() != null ? turno.getFecha() : fechaConsulta, turno.getHoraInicio());
    }

    private LocalDateTime finTurno(Turno turno, LocalDate fechaConsulta) {
        LocalDateTime inicio = inicioTurno(turno, fechaConsulta);
        return LocalDateTime.of(esNocturno(turno) ? inicio.toLocalDate().plusDays(1) : inicio.toLocalDate(), turno.getHoraFin());
    }

    private boolean esNocturno(Turno turno) {
        return !turno.getHoraFin().isAfter(turno.getHoraInicio());
    }

    private boolean esMedico(Trabajador trabajador) {
        return trabajador.getRol() != null && "MEDICO".equalsIgnoreCase(trabajador.getRol().getNombre());
    }

    private Trabajador obtenerTrabajadorAutenticado(Long creadoPorIdFallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return trabajadorRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
        }
        if (creadoPorIdFallback != null) {
            return trabajadorRepository.findById(creadoPorIdFallback)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario creador no encontrado."));
        }
        throw new IllegalArgumentException("No se pudo identificar al usuario.");
    }

    private String generarNumeroCita(LocalDateTime fechaHora) {
        return "CT-" + fechaHora.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%06d", Math.abs(System.nanoTime() % 1_000_000));
    }

    private Turno.DiaSemana convertirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> Turno.DiaSemana.LUNES;
            case TUESDAY -> Turno.DiaSemana.MARTES;
            case WEDNESDAY -> Turno.DiaSemana.MIERCOLES;
            case THURSDAY -> Turno.DiaSemana.JUEVES;
            case FRIDAY -> Turno.DiaSemana.VIERNES;
            case SATURDAY -> Turno.DiaSemana.SABADO;
            case SUNDAY -> Turno.DiaSemana.DOMINGO;
        };
    }

    private TrabajadorResponseDTO mapTrabajadorToDTO(Trabajador trabajador) {
        List<String> especialidades = trabajador.getEspecialidades() != null
                ? trabajador.getEspecialidades().stream().map(Especialidad::getNombre).collect(Collectors.toList())
                : List.of();

        return TrabajadorResponseDTO.builder()
                .id(trabajador.getId())
                .dni(trabajador.getDni())
                .nombreCompleto(trabajador.getNombreCompleto())
                .username(trabajador.getUsername())
                .email(trabajador.getEmail())
                .telefono(trabajador.getTelefono())
                .fechaNacimiento(trabajador.getFechaNacimiento())
                .colegiatura(trabajador.getColegiatura())
                .rolId(trabajador.getRol().getId())
                .nombreRol(trabajador.getRol().getNombre())
                .activo(trabajador.isActivo())
                .especialidades(especialidades)
                .build();
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