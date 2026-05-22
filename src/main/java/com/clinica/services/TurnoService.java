package com.clinica.services;

import com.clinica.dtos.TurnoRequestDTO;
import com.clinica.dtos.TurnoResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.Consultorio;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.entities.Turno;
import com.clinica.model.repositories.ConsultorioRepository;
import com.clinica.model.repositories.EspecialidadRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.model.repositories.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final int MAX_HORAS_JORNADA = 12;

    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultorioRepository consultorioRepository;

    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listar(Long especialidadId, int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.with(TemporalAdjusters.lastDayOfMonth());
        List<Turno> turnos = especialidadId == null
                ? turnoRepository.findByFechaBetweenAndActivoTrue(inicio, fin)
                : turnoRepository.findByEspecialidadAndFechaBetween(especialidadId, inicio, fin);

        return turnos
                .stream()
                .map(t -> map(t, especialidadId))
                .toList();
    }

    @Transactional
    public TurnoResponseDTO crear(TurnoRequestDTO dto) {
        validarBasico(dto);

        Trabajador medico = trabajadorRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Medico no encontrado."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada."));

        validarMedicoEspecialidad(medico, especialidad);
        LocalDateTime inicioNuevo = inicio(dto);
        LocalDateTime finNuevo = fin(dto);

        validarDisponibilidad(null, medico.getId(), especialidad, inicioNuevo, finNuevo);
        Consultorio consultorio = buscarConsultorioDisponible(especialidad.getId(), inicioNuevo, finNuevo, null);

        Turno turno = Turno.builder()
                .medico(medico)
                .consultorio(consultorio)
                .fecha(dto.getFecha())
                .diaSemana(convertirDiaSemana(dto.getFecha().getDayOfWeek()))
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .cupoMaximo((int) (Duration.between(inicioNuevo, finNuevo).toMinutes() / 30))
                .activo(true)
                .build();

        return map(turnoRepository.save(turno), especialidad.getId());
    }

    @Transactional
    public TurnoResponseDTO actualizar(Long id, TurnoRequestDTO dto) {
        validarBasico(dto);

        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado."));
        Trabajador medico = trabajadorRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Medico no encontrado."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada."));

        validarMedicoEspecialidad(medico, especialidad);
        LocalDateTime inicioNuevo = inicio(dto);
        LocalDateTime finNuevo = fin(dto);

        validarDisponibilidad(id, medico.getId(), especialidad, inicioNuevo, finNuevo);
        Consultorio consultorio = buscarConsultorioDisponible(especialidad.getId(), inicioNuevo, finNuevo, id);

        turno.setMedico(medico);
        turno.setConsultorio(consultorio);
        turno.setFecha(dto.getFecha());
        turno.setDiaSemana(convertirDiaSemana(dto.getFecha().getDayOfWeek()));
        turno.setHoraInicio(dto.getHoraInicio());
        turno.setHoraFin(dto.getHoraFin());
        turno.setCupoMaximo((int) (Duration.between(inicioNuevo, finNuevo).toMinutes() / 30));
        turno.setActivo(true);

        return map(turnoRepository.save(turno), especialidad.getId());
    }

    @Transactional
    public void eliminar(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado."));
        turnoRepository.delete(turno);
    }

    private void validarBasico(TurnoRequestDTO dto) {
        if (dto.getMedicoId() == null || dto.getEspecialidadId() == null || dto.getFecha() == null
                || dto.getHoraInicio() == null || dto.getHoraFin() == null) {
            throw new IllegalArgumentException("Complete especialidad, medico, fecha y horas.");
        }
        long minutos = Duration.between(inicio(dto), fin(dto)).toMinutes();
        if (minutos > MAX_HORAS_JORNADA * 60L) {
            throw new IllegalArgumentException("La jornada maxima permitida es de 12 horas.");
        }
        if (minutos < 30 || minutos % 30 != 0) {
            throw new IllegalArgumentException("El turno debe durar al menos 30 minutos y cerrar en bloques de 30.");
        }
    }

    private void validarMedicoEspecialidad(Trabajador medico, Especialidad especialidad) {
        if (!esMedico(medico) || !medico.isActivo()) {
            throw new IllegalArgumentException("Solo se pueden asignar turnos a medicos activos.");
        }
        boolean pertenece = medico.getEspecialidades().stream().anyMatch(e -> e.getId().equals(especialidad.getId()));
        if (!pertenece) {
            throw new IllegalArgumentException("El medico no pertenece a la especialidad seleccionada.");
        }
    }

    private void validarDisponibilidad(
            Long turnoIgnoradoId,
            Long medicoId,
            Especialidad especialidad,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo) {
        if (tieneCruceMedico(medicoId, inicioNuevo, finNuevo, turnoIgnoradoId)) {
            throw new IllegalStateException("El medico ya tiene un turno que se cruza con ese horario.");
        }

        int maxMedicos = "Medicina General".equalsIgnoreCase(especialidad.getNombre()) ? 2 : 1;
        long ocupadosEspecialidad = contarMedicosCruzadosEspecialidad(
                especialidad.getId(), inicioNuevo, finNuevo, turnoIgnoradoId);
        if (ocupadosEspecialidad >= maxMedicos) {
            throw new IllegalStateException(
                    "La especialidad ya alcanzo el maximo de medicos simultaneos para ese bloque.");
        }
    }

    private Consultorio buscarConsultorioDisponible(
            Long especialidadId,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo,
            Long turnoIgnoradoId) {
        List<Consultorio> candidatos = consultorioRepository.findByEspecialidadIdAndActivoTrueOrderByIdAsc(especialidadId);
        if (candidatos.isEmpty()) {
            candidatos = consultorioRepository.findByActivoTrueOrderByIdAsc();
        }
        return candidatos.stream()
                .filter(c -> !tieneCruceConsultorio(c.getId(), inicioNuevo, finNuevo, turnoIgnoradoId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay consultorios disponibles para ese bloque horario."));
    }

    private boolean tieneCruceMedico(
            Long medicoId,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo,
            Long turnoIgnoradoId) {
        LocalDate inicio = inicioNuevo.toLocalDate().minusDays(1);
        LocalDate fin = finNuevo.toLocalDate().plusDays(1);
        return turnoRepository.findByMedicoIdAndFechaBetweenAndActivoTrue(medicoId, inicio, fin).stream()
                .filter(t -> !esTurnoIgnorado(t, turnoIgnoradoId))
                .anyMatch(t -> seCruzan(inicioTurno(t), finTurno(t), inicioNuevo, finNuevo));
    }

    private boolean tieneCruceConsultorio(
            Long consultorioId,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo,
            Long turnoIgnoradoId) {
        LocalDate inicio = inicioNuevo.toLocalDate().minusDays(1);
        LocalDate fin = finNuevo.toLocalDate().plusDays(1);
        return turnoRepository.findByConsultorioIdAndFechaBetweenAndActivoTrue(consultorioId, inicio, fin).stream()
                .filter(t -> !esTurnoIgnorado(t, turnoIgnoradoId))
                .anyMatch(t -> seCruzan(inicioTurno(t), finTurno(t), inicioNuevo, finNuevo));
    }

    private long contarMedicosCruzadosEspecialidad(
            Long especialidadId,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo,
            Long turnoIgnoradoId) {
        LocalDate inicio = inicioNuevo.toLocalDate().minusDays(1);
        LocalDate fin = finNuevo.toLocalDate().plusDays(1);
        Set<Long> medicos = new HashSet<>();
        turnoRepository.findByEspecialidadAndFechaBetween(especialidadId, inicio, fin).forEach(t -> {
            if (!esTurnoIgnorado(t, turnoIgnoradoId)
                    && seCruzan(inicioTurno(t), finTurno(t), inicioNuevo, finNuevo)) {
                medicos.add(t.getMedico().getId());
            }
        });
        return medicos.size();
    }

    private boolean seCruzan(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    private LocalDateTime inicio(TurnoRequestDTO dto) {
        return LocalDateTime.of(dto.getFecha(), dto.getHoraInicio());
    }

    private LocalDateTime fin(TurnoRequestDTO dto) {
        LocalDate fechaFin = dto.getHoraFin().isAfter(dto.getHoraInicio())
                ? dto.getFecha()
                : dto.getFecha().plusDays(1);
        return LocalDateTime.of(fechaFin, dto.getHoraFin());
    }

    private LocalDateTime inicioTurno(Turno turno) {
        return LocalDateTime.of(turno.getFecha(), turno.getHoraInicio());
    }

    private LocalDateTime finTurno(Turno turno) {
        LocalDate fechaFin = turno.getHoraFin().isAfter(turno.getHoraInicio())
                ? turno.getFecha()
                : turno.getFecha().plusDays(1);
        return LocalDateTime.of(fechaFin, turno.getHoraFin());
    }

    private boolean esTurnoIgnorado(Turno turno, Long turnoIgnoradoId) {
        return turnoIgnoradoId != null && turno.getId().equals(turnoIgnoradoId);
    }

    private TurnoResponseDTO map(Turno turno, Long especialidadId) {
        Especialidad especialidad = resolverEspecialidad(turno, especialidadId);

        return TurnoResponseDTO.builder()
                .id(turno.getId())
                .medicoId(turno.getMedico().getId())
                .nombreMedico(turno.getMedico().getNombreCompleto())
                .especialidadId(especialidad != null ? especialidad.getId() : null)
                .especialidad(especialidad != null ? especialidad.getNombre() : "Especialidad")
                .consultorioId(turno.getConsultorio().getId())
                .consultorio(turno.getConsultorio().getNombre())
                .fecha(turno.getFecha())
                .diaSemana(turno.getDiaSemana().name())
                .horaInicio(turno.getHoraInicio())
                .horaFin(turno.getHoraFin())
                .activo(turno.isActivo())
                .build();
    }

    private Especialidad resolverEspecialidad(Turno turno, Long especialidadId) {
        if (especialidadId != null) {
            return turno.getMedico().getEspecialidades().stream()
                    .filter(e -> e.getId().equals(especialidadId))
                    .findFirst()
                    .orElse(turno.getConsultorio().getEspecialidad());
        }
        if (turno.getConsultorio().getEspecialidad() != null) {
            return turno.getConsultorio().getEspecialidad();
        }
        return turno.getMedico().getEspecialidades().stream()
                .findFirst()
                .orElse(null);
    }

    private boolean esMedico(Trabajador trabajador) {
        return trabajador.getRol() != null
                && "MEDICO".equalsIgnoreCase(trabajador.getRol().getNombre());
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
}
