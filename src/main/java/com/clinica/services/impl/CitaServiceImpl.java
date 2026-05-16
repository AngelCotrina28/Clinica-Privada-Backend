package com.clinica.services.impl;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.HorarioBloqueDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.HistoriaClinica;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.TipoCita;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.entities.Turno;
import com.clinica.model.repositories.CitaRepository;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.PacienteRepository;
import com.clinica.model.repositories.TipoCitaRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.model.repositories.TurnoRepository;
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

    private static final int DURACION_BLOQUE_MINUTOS = 30;
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
        validarRequest(request);

        Trabajador creador = obtenerTrabajadorAutenticado(request.getCreadoPorId());
        Paciente paciente = obtenerOCrearPacienteDesdeHistoria(request.getHistoriaClinicaId(), creador);

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
        if (especialidadId == null) {
            throw new IllegalArgumentException("Debe seleccionar una especialidad.");
        }

        return turnoRepository.findMedicosActivosByEspecialidad(especialidadId).stream()
                .map(this::mapTrabajadorToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioBloqueDTO> obtenerDisponibilidad(Long medicoId, LocalDate fecha) {
        if (medicoId == null || fecha == null) {
            throw new IllegalArgumentException("Debe seleccionar medico y fecha.");
        }

        Turno.DiaSemana diaSemana = convertirDiaSemana(fecha.getDayOfWeek());
        List<Turno> turnos = turnoRepository.findTurnosActivosDelMedico(medicoId, fecha, fecha.minusDays(1), diaSemana);
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        Map<LocalDateTime, Cita> citasOcupadas = citaRepository
                .findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(
                        medicoId, inicioDia, finDia, ESTADOS_QUE_BLOQUEAN)
                .stream()
                .collect(Collectors.toMap(Cita::getFechaHoraCita, Function.identity(), (a, b) -> a));

        return turnos.stream()
                .flatMap(turno -> generarBloques(fecha, turno, citasOcupadas).stream())
                .collect(Collectors.toList());
    }

    private List<HorarioBloqueDTO> generarBloques(
            LocalDate fecha,
            Turno turno,
            Map<LocalDateTime, Cita> citasOcupadas) {

        java.util.ArrayList<HorarioBloqueDTO> bloques = new java.util.ArrayList<>();
        LocalDateTime inicioTurno = inicioTurno(turno, fecha);
        LocalDateTime finTurno = finTurno(turno, fecha);
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        LocalDateTime cursor = inicioTurno.isBefore(inicioDia) ? inicioDia : inicioTurno;

        while (!cursor.plusMinutes(DURACION_BLOQUE_MINUTOS).isAfter(finTurno) && cursor.isBefore(finDia)) {
            LocalDateTime fechaHora = cursor;
            Cita citaOcupada = citasOcupadas.get(fechaHora);
            boolean disponible = citaOcupada == null && !fechaHora.isBefore(LocalDateTime.now());

            bloques.add(HorarioBloqueDTO.builder()
                    .turnoId(turno.getId())
                    .consultorioId(turno.getConsultorio().getId())
                    .consultorio(turno.getConsultorio().getNombre())
                    .horaInicio(fechaHora.toLocalTime())
                    .horaFin(fechaHora.plusMinutes(DURACION_BLOQUE_MINUTOS).toLocalTime())
                    .disponible(disponible)
                    .estado(disponible ? "DISPONIBLE" : "OCUPADO")
                    .citaId(citaOcupada != null ? citaOcupada.getId() : null)
                    .numeroCita(citaOcupada != null ? citaOcupada.getNumeroCita() : null)
                    .build());

            cursor = cursor.plusMinutes(DURACION_BLOQUE_MINUTOS);
        }

        return bloques;
    }

    private Paciente obtenerOCrearPacienteDesdeHistoria(Long historiaClinicaId, Trabajador registrador) {
        return pacienteRepository.findByHistoriaClinicaId(historiaClinicaId)
                .orElseGet(() -> {
                    HistoriaClinica historia = historiaClinicaRepository.findById(historiaClinicaId)
                            .orElseThrow(() -> new RecursoNoEncontradoException(
                                    "Historia clinica no encontrada con ID: " + historiaClinicaId));

                    return pacienteRepository.findByDni(historia.getDniPaciente())
                            .map(paciente -> vincularPacienteConHistoria(paciente, historia))
                            .orElseGet(() -> crearPacienteDesdeHistoria(historia, registrador));
                });
    }

    private Paciente vincularPacienteConHistoria(Paciente paciente, HistoriaClinica historia) {
        if (!paciente.isActivo()) {
            throw new IllegalStateException("El paciente asociado a la historia clinica esta inactivo.");
        }
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
                .activo(true)
                .build();
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
        if (fechaNacimiento == null || fechaNacimiento.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaNacimiento.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String valorOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validarRequest(CitaRequestDTO request) {
        if (request.getHistoriaClinicaId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una historia clinica.");
        }
        if (request.getMedicoId() == null) {
            throw new IllegalArgumentException("Debe seleccionar un medico.");
        }
        if (request.getTurnoId() == null) {
            throw new IllegalArgumentException("Debe seleccionar un bloque horario.");
        }
        if (request.getFechaHora() == null) {
            throw new IllegalArgumentException("Debe seleccionar fecha y hora.");
        }
    }

    private void validarTurno(CitaRequestDTO request, Trabajador medico, Turno turno) {
        if (!esMedico(turno.getMedico())) {
            throw new IllegalArgumentException("El turno seleccionado no pertenece a un medico.");
        }
        if (!turno.isActivo()) {
            throw new IllegalStateException("El turno seleccionado no esta activo.");
        }
        if (!turno.getMedico().getId().equals(medico.getId())) {
            throw new IllegalArgumentException("El turno seleccionado no pertenece al medico.");
        }
        if (request.getEspecialidadId() != null) {
            boolean pertenece = medico.getEspecialidades().stream()
                    .anyMatch(e -> e.getId().equals(request.getEspecialidadId()));
            if (!pertenece) {
                throw new IllegalArgumentException("El medico no pertenece a la especialidad seleccionada.");
            }
        }

        LocalDate fecha = request.getFechaHora().toLocalDate();
        LocalTime hora = request.getFechaHora().toLocalTime();
        LocalDateTime inicioTurno = inicioTurno(turno, fecha);
        LocalDateTime finTurno = finTurno(turno, fecha);
        LocalDateTime fechaHoraSolicitada = request.getFechaHora();

        if (turno.getFecha() != null
                && !turno.getFecha().equals(fecha)
                && !(esNocturno(turno) && turno.getFecha().plusDays(1).equals(fecha))) {
            throw new IllegalArgumentException("El turno no corresponde a la fecha seleccionada.");
        }
        if (!esNocturno(turno) && turno.getDiaSemana() != convertirDiaSemana(fecha.getDayOfWeek())) {
            throw new IllegalArgumentException("El turno no corresponde al dia seleccionado.");
        }
        if (fechaHoraSolicitada.isBefore(inicioTurno)
                || fechaHoraSolicitada.plusMinutes(DURACION_BLOQUE_MINUTOS).isAfter(finTurno)) {
            throw new IllegalArgumentException("La hora seleccionada esta fuera del turno del medico.");
        }
        if (hora.getMinute() % DURACION_BLOQUE_MINUTOS != 0) {
            throw new IllegalArgumentException("La hora seleccionada debe coincidir con un bloque de 30 minutos.");
        }
    }

    private TipoCita obtenerTipoConsultaExterna() {
        return tipoCitaRepository.findFirstByNombreIgnoreCaseAndActivoTrue("CONSULTA EXTERNA")
                .or(() -> tipoCitaRepository.findFirstByActivoTrueOrderByIdAsc())
                .orElseGet(() -> tipoCitaRepository.save(TipoCita.builder()
                        .nombre("CONSULTA EXTERNA")
                        .descripcion("Cita de consulta externa")
                        .duracionMinutos(DURACION_BLOQUE_MINUTOS)
                        .activo(true)
                        .build()));
    }

    private LocalDateTime inicioTurno(Turno turno, LocalDate fechaConsulta) {
        LocalDate fechaInicio = turno.getFecha() != null ? turno.getFecha() : fechaConsulta;
        return LocalDateTime.of(fechaInicio, turno.getHoraInicio());
    }

    private LocalDateTime finTurno(Turno turno, LocalDate fechaConsulta) {
        LocalDateTime inicio = inicioTurno(turno, fechaConsulta);
        LocalDate fechaFin = esNocturno(turno) ? inicio.toLocalDate().plusDays(1) : inicio.toLocalDate();
        return LocalDateTime.of(fechaFin, turno.getHoraFin());
    }

    private boolean esNocturno(Turno turno) {
        return !turno.getHoraFin().isAfter(turno.getHoraInicio());
    }

    private boolean esMedico(Trabajador trabajador) {
        return trabajador.getRol() != null
                && trabajador.getRol().getNombre() != null
                && "MEDICO".equalsIgnoreCase(trabajador.getRol().getNombre());
    }

    private Trabajador obtenerTrabajadorAutenticado(Long creadoPorIdFallback) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            return trabajadorRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
        }

        if (creadoPorIdFallback != null) {
            return trabajadorRepository.findById(creadoPorIdFallback)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario creador no encontrado."));
        }

        throw new IllegalArgumentException("No se pudo identificar al usuario que programa la cita.");
    }

    private String generarNumeroCita(LocalDateTime fechaHora) {
        String fechaStr = fechaHora.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "CT-" + fechaStr + "-" + String.format("%06d", Math.abs(System.nanoTime() % 1_000_000));
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
                ? trabajador.getEspecialidades().stream()
                        .map(Especialidad::getNombre)
                        .collect(Collectors.toList())
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
