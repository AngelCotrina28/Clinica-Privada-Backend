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
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock
    TurnoRepository turnoRepository;

    @Mock
    TrabajadorRepository trabajadorRepository;

    @Mock
    EspecialidadRepository especialidadRepository;

    @Mock
    ConsultorioRepository consultorioRepository;

    @InjectMocks
    TurnoService turnoService;

    @Test
    void crearTurnoValidoAsignaConsultorioDisponibleYCalculaCupos() {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Especialidad especialidad = TestFixtures.especialidad(10L, "Medicina General");
        Trabajador medico = TestFixtures.medico(10L);
        Consultorio consultorio = TestFixtures.consultorio(1L, especialidad);
        TurnoRequestDTO dto = TestFixtures.turnoRequest(fecha, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(especialidad));
        when(turnoRepository.findByMedicoIdAndFechaBetweenAndActivoTrue(eq(10L), any(), any())).thenReturn(List.of());
        when(turnoRepository.findByEspecialidadAndFechaBetween(eq(10L), any(), any())).thenReturn(List.of());
        when(consultorioRepository.findByEspecialidadIdAndActivoTrueOrderByIdAsc(10L)).thenReturn(List.of(consultorio));
        when(turnoRepository.findByConsultorioIdAndFechaBetweenAndActivoTrue(eq(1L), any(), any())).thenReturn(List.of());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno turno = invocation.getArgument(0);
            turno.setId(100L);
            return turno;
        });

        TurnoResponseDTO response = turnoService.crear(dto);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getConsultorio()).isEqualTo("Consultorio 1");
        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepository).save(captor.capture());
        assertThat(captor.getValue().getCupoMaximo()).isEqualTo(4);
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    void crearConDatosIncompletosLanzaIllegalArgumentException() {
        TurnoRequestDTO dto = new TurnoRequestDTO();
        dto.setMedicoId(10L);

        assertThatThrownBy(() -> turnoService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Complete");
    }

    @Test
    void crearConJornadaMayorADoceHorasLanzaIllegalArgumentException() {
        TurnoRequestDTO dto = TestFixtures.turnoRequest(
                LocalDate.now().plusDays(3), LocalTime.of(6, 0), LocalTime.of(19, 0));

        assertThatThrownBy(() -> turnoService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 horas");
    }

    @Test
    void crearConMedicoNoPertenecienteAEspecialidadLanzaIllegalArgumentException() {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Trabajador medico = TestFixtures.medico(10L);
        Especialidad pediatria = TestFixtures.especialidad(99L, "Pediatria");
        TurnoRequestDTO dto = TestFixtures.turnoRequest(fecha, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(pediatria));

        assertThatThrownBy(() -> turnoService.crear(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void crearConCruceDeHorarioDelMedicoLanzaIllegalStateException() {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Especialidad especialidad = TestFixtures.especialidad(10L, "Medicina General");
        Trabajador medico = TestFixtures.medico(10L);
        Consultorio consultorio = TestFixtures.consultorio(1L, especialidad);
        Turno existente = TestFixtures.turno(1L, medico, consultorio, fecha, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TurnoRequestDTO dto = TestFixtures.turnoRequest(fecha, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(especialidad));
        when(turnoRepository.findByMedicoIdAndFechaBetweenAndActivoTrue(eq(10L), any(), any())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> turnoService.crear(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("se cruza");
    }

    @Test
    void crearSinConsultoriosDisponiblesLanzaIllegalStateException() {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Especialidad especialidad = TestFixtures.especialidad(10L, "Medicina General");
        Trabajador medico = TestFixtures.medico(10L);
        TurnoRequestDTO dto = TestFixtures.turnoRequest(fecha, LocalTime.of(8, 0), LocalTime.of(10, 0));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(especialidadRepository.findById(10L)).thenReturn(Optional.of(especialidad));
        when(turnoRepository.findByMedicoIdAndFechaBetweenAndActivoTrue(eq(10L), any(), any())).thenReturn(List.of());
        when(turnoRepository.findByEspecialidadAndFechaBetween(eq(10L), any(), any())).thenReturn(List.of());
        when(consultorioRepository.findByEspecialidadIdAndActivoTrueOrderByIdAsc(10L)).thenReturn(List.of());
        when(consultorioRepository.findByEspecialidadIsNullAndActivoTrueOrderByIdAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> turnoService.crear(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay consultorios activos");
    }

    @Test
    void eliminarTurnoInexistenteLanzaRecursoNoEncontrado() {
        when(turnoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarPorEspecialidadUsaRangoDelMes() {
        LocalDate fecha = LocalDate.of(2026, 6, 10);
        Especialidad especialidad = TestFixtures.especialidad(10L, "Medicina General");
        Trabajador medico = TestFixtures.medico(10L);
        Consultorio consultorio = TestFixtures.consultorio(1L, especialidad);
        when(turnoRepository.findByEspecialidadAndFechaBetween(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(TestFixtures.turno(1L, medico, consultorio, fecha, LocalTime.of(8, 0), LocalTime.of(10, 0))));

        List<TurnoResponseDTO> response = turnoService.listar(10L, 2026, 6);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFecha()).isEqualTo(fecha);
    }
}
