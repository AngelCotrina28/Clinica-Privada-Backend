package com.clinica.model.repositories;

import com.clinica.model.entities.Trabajador;
import com.clinica.model.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    @Query("""
            select distinct t.medico
            from Turno t
            join t.medico.especialidades e
            where t.medico.activo = true
              and upper(t.medico.rol.nombre) = 'MEDICO'
              and t.activo = true
              and t.fecha is not null
              and e.id = :especialidadId
            order by t.medico.nombreCompleto
            """)
    List<Trabajador> findMedicosActivosByEspecialidad(@Param("especialidadId") Long especialidadId);

    @Query("""
            select t
            from Turno t
            join fetch t.consultorio c
            where t.medico.id = :medicoId
              and (t.fecha = :fecha or t.fecha = :fechaAnterior or (t.fecha is null and t.diaSemana = :diaSemana))
              and t.activo = true
              and t.medico.activo = true
              and upper(t.medico.rol.nombre) = 'MEDICO'
              and c.activo = true
            order by t.horaInicio
            """)
    List<Turno> findTurnosActivosDelMedico(
            @Param("medicoId") Long medicoId,
            @Param("fecha") LocalDate fecha,
            @Param("fechaAnterior") LocalDate fechaAnterior,
            @Param("diaSemana") Turno.DiaSemana diaSemana);

    @Query("""
            select t
            from Turno t
            join fetch t.medico m
            join fetch t.consultorio c
            left join fetch c.especialidad ce
            left join m.especialidades e
            where t.fecha between :inicio and :fin
              and (ce.id = :especialidadId or (ce is null and e.id = :especialidadId))
              and t.activo = true
            order by t.fecha, t.horaInicio, m.nombreCompleto
            """)
    List<Turno> findByEspecialidadAndFechaBetween(
            @Param("especialidadId") Long especialidadId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    @Query("""
            select count(t)
            from Turno t
            where t.medico.id = :medicoId
              and t.fecha = :fecha
              and t.activo = true
              and t.horaInicio < :horaFin
              and t.horaFin > :horaInicio
            """)
    long countSolapadosMedico(
            @Param("medicoId") Long medicoId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    List<Turno> findByMedicoIdAndFechaBetweenAndActivoTrue(Long medicoId, LocalDate inicio, LocalDate fin);

    List<Turno> findByConsultorioIdAndFechaBetweenAndActivoTrue(Long consultorioId, LocalDate inicio, LocalDate fin);

    @Query("""
            select t
            from Turno t
            join fetch t.medico m
            join fetch t.consultorio c
            left join fetch c.especialidad ce
            where t.fecha between :inicio and :fin
              and t.activo = true
            order by t.fecha, t.horaInicio, m.nombreCompleto
            """)
    List<Turno> findByFechaBetweenAndActivoTrue(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    @Query("""
            select count(distinct t.medico.id)
            from Turno t
            join t.medico.especialidades e
            where e.id = :especialidadId
              and t.fecha = :fecha
              and t.activo = true
              and t.horaInicio < :horaFin
              and t.horaFin > :horaInicio
            """)
    long countMedicosSolapadosEspecialidad(
            @Param("especialidadId") Long especialidadId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
