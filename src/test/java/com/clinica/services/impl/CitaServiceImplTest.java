package com.clinica.services.impl;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.HorarioBloqueDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.Consultorio;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.HistoriaClinica;
import com.clinica.model.entities.TipoCita;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.entities.Turno;
import com.clinica.model.repositories.CitaRepository;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.PacienteRepository;
import com.clinica.model.repositories.TipoCitaRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.model.repositories.TurnoRepository;
import com.clinica.services.DeudaService;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock
    CitaRepository citaRepository;

    @Mock
    HistoriaClinicaRepository historiaClinicaRepository;

    @Mock
    PacienteRepository pacienteRepository;

    @Mock
    TrabajadorRepository trabajadorRepository;

    @Mock
    TipoCitaRepository tipoCitaRepository;

    @Mock
    TurnoRepository turnoRepository;

    @Mock
    DeudaService deudaService;

    @InjectMocks
    CitaServiceImpl citaService;

    @AfterEach
    void tearDown() {
        TestFixtures.limpiarAutenticacion();
    }

    @Test
    void programarCitaValidaGuardaCitaProgramadaYCreaDeuda() {
        TestFixtures.autenticarComo("admision");
        LocalDate fecha = LocalDate.now().plusDays(7);
        LocalDateTime fechaHora = LocalDateTime.of(fecha, LocalTime.of(9, 0));
        Especialidad especialidad = TestFixtures.especialidad(10L, "Medicina General");
        Trabajador medico = TestFixtures.medico(10L);
        Consultorio consultorio = TestFixtures.consultorio(1L, especialidad);
        Turno turno = TestFixtures.turno(5L, medico, consultorio, fecha, LocalTime.of(8, 0), LocalTime.of(12, 0));
        HistoriaClinica historia = TestFixtures.historia(1L);
        TipoCita tipoCita = TestFixtures.tipoCita();
        when(trabajadorRepository.findByUsername("admision")).thenReturn(Optional.of(TestFixtures.trabajador(99L, "admision", "ADMISION", true)));
        when(historiaClinicaRepository.findById(1L)).thenReturn(Optional.of(historia));
        when(pacienteRepository.findByHistoriaClinicaId(1L)).thenReturn(Optional.of(historia.getPaciente()));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(turno));
        when(citaRepository.existsByMedicoIdAndFechaHoraCitaAndEstadoIn(eq(10L), eq(fechaHora), anyCollection()))
                .thenReturn(false);
        when(tipoCitaRepository.findFirstByNombreIgnoreCaseAndActivoTrue("CONSULTA EXTERNA"))
                .thenReturn(Optional.of(tipoCita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita cita = invocation.getArgument(0);
            cita.setId(100L);
            return cita;
        });

        CitaResponseDTO response = citaService.programarCita(TestFixtures.citaRequest(fechaHora, 5L));

        assertThat(response.getEstado()).isEqualTo("PROGRAMADA");
        assertThat(response.getNombreMedico()).isEqualTo(medico.getNombreCompleto());
        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroCita()).startsWith("CT-");
        assertThat(captor.getValue().getConsultorio()).isSameAs(consultorio);
        verify(deudaService).asegurarDeudaCita(eq(captor.getValue()), any(Trabajador.class));
    }

    @Test
    void programarCitaSinMedicoLanzaIllegalArgumentException() {
        CitaRequestDTO request = TestFixtures.citaRequest(LocalDateTime.now().plusDays(1), 5L);
        request.setMedicoId(null);

        assertThatThrownBy(() -> citaService.programarCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("medico");
    }

    @Test
    void programarCitaConHistoriaInexistenteLanzaRecursoNoEncontrado() {
        TestFixtures.autenticarComo("admision");
        when(trabajadorRepository.findByUsername("admision")).thenReturn(Optional.of(TestFixtures.trabajador(99L, "admision", "ADMISION", true)));
        when(historiaClinicaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.programarCita(
                TestFixtures.citaRequest(LocalDateTime.now().plusDays(1), 5L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Historia");
    }

    @Test
    void programarCitaConMedicoInactivoLanzaIllegalStateException() {
        TestFixtures.autenticarComo("admision");
        Trabajador medico = TestFixtures.medico(10L);
        medico.setActivo(false);
        when(trabajadorRepository.findByUsername("admision")).thenReturn(Optional.of(TestFixtures.trabajador(99L, "admision", "ADMISION", true)));
        when(historiaClinicaRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(pacienteRepository.findByHistoriaClinicaId(1L)).thenReturn(Optional.of(TestFixtures.paciente(1L)));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> citaService.programarCita(
                TestFixtures.citaRequest(LocalDateTime.now().plusDays(1), 5L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no esta activo");
    }

    @Test
    void programarCitaConTurnoDeOtroMedicoLanzaIllegalArgumentException() {
        TestFixtures.autenticarComo("admision");
        LocalDate fecha = LocalDate.now().plusDays(7);
        LocalDateTime fechaHora = LocalDateTime.of(fecha, LocalTime.of(9, 0));
        Trabajador medico = TestFixtures.medico(10L);
        Trabajador otroMedico = TestFixtures.medico(20L);
        Turno turno = TestFixtures.turno(5L, otroMedico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                fecha, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(trabajadorRepository.findByUsername("admision")).thenReturn(Optional.of(TestFixtures.trabajador(99L, "admision", "ADMISION", true)));
        when(historiaClinicaRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(pacienteRepository.findByHistoriaClinicaId(1L)).thenReturn(Optional.of(TestFixtures.paciente(1L)));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(turno));

        assertThatThrownBy(() -> citaService.programarCita(TestFixtures.citaRequest(fechaHora, 5L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Turno no pertenece");
    }

    @Test
    void programarCitaConBloqueOcupadoLanzaIllegalStateException() {
        TestFixtures.autenticarComo("admision");
        LocalDate fecha = LocalDate.now().plusDays(7);
        LocalDateTime fechaHora = LocalDateTime.of(fecha, LocalTime.of(9, 0));
        Trabajador medico = TestFixtures.medico(10L);
        Turno turno = TestFixtures.turno(5L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                fecha, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(trabajadorRepository.findByUsername("admision")).thenReturn(Optional.of(TestFixtures.trabajador(99L, "admision", "ADMISION", true)));
        when(historiaClinicaRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(pacienteRepository.findByHistoriaClinicaId(1L)).thenReturn(Optional.of(TestFixtures.paciente(1L)));
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(medico));
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(turno));
        when(citaRepository.existsByMedicoIdAndFechaHoraCitaAndEstadoIn(eq(10L), eq(fechaHora), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> citaService.programarCita(TestFixtures.citaRequest(fechaHora, 5L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ocupado");
    }

    @Test
    void obtenerDisponibilidadDevuelveBloquesDisponiblesYOcupados() {
        LocalDate fecha = LocalDate.now().plusDays(7);
        Trabajador medico = TestFixtures.medico(10L);
        Turno turno = TestFixtures.turno(5L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                fecha, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Cita ocupada = TestFixtures.cita(1L, TestFixtures.historia(1L), medico, turno,
                LocalDateTime.of(fecha, LocalTime.of(9, 0)));
        when(turnoRepository.findTurnosActivosDelMedico(eq(10L), eq(fecha), eq(fecha.minusDays(1)), any()))
                .thenReturn(List.of(turno));
        when(citaRepository.findByMedicoIdAndFechaHoraCitaBetweenAndEstadoIn(eq(10L), any(), any(), anyCollection()))
                .thenReturn(List.of(ocupada));

        List<HorarioBloqueDTO> bloques = citaService.obtenerDisponibilidad(10L, fecha);

        assertThat(bloques).hasSize(2);
        assertThat(bloques.get(0).isDisponible()).isFalse();
        assertThat(bloques.get(0).getEstado()).isEqualTo("OCUPADO");
        assertThat(bloques.get(1).isDisponible()).isTrue();
    }
}
