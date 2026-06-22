package com.clinica.services;

import com.clinica.dtos.AtencionMedicaRequestDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.entities.Cita;
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
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtencionMedicaServiceTest {

    @Mock
    AtencionMedicaRepository atencionRepo;

    @Mock
    HistoriaClinicaRepository historiaRepo;

    @Mock
    TrabajadorRepository trabajadorRepo;

    @Mock
    CitaRepository citaRepo;

    @Mock
    OrdenAtencionEmergenciaRepository ordenAtencionEmergenciaRepo;

    @Mock
    MedicamentoRepository medicamentoRepo;

    @Mock
    RecetaRepository recetaRepo;

    @InjectMocks
    AtencionMedicaService atencionService;

    @Test
    void verificarEstadoCitaUOrdenVacioRetornaNoExiste() {
        assertThat(atencionService.verificarEstadoCitaUOrden(" ")).isEqualTo("NO_EXISTE");
    }

    @Test
    void verificarEstadoCitaConfirmadaDeHoyRetornaValida() {
        HistoriaClinica historia = TestFixtures.historia(1L);
        Trabajador medico = TestFixtures.medico(10L);
        var turno = TestFixtures.turno(1L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                java.time.LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(12, 0));
        Cita cita = TestFixtures.cita(1L, historia, medico, turno, LocalDateTime.now());
        cita.setEstado(Cita.EstadoCita.CONFIRMADA);
        when(citaRepo.findByNumeroCita("CT-1")).thenReturn(Optional.of(cita));

        assertThat(atencionService.verificarEstadoCitaUOrden("CT-1")).isEqualTo("VALIDA");
    }

    @Test
    void verificarEstadoOrdenFinalizadaDeHoyRetornaAtendida() {
        OrdenAtencionEmergencia orden = TestFixtures.ordenEmergencia(1L, TestFixtures.historia(1L), TestFixtures.medico(10L));
        orden.setEstado(OrdenAtencionEmergencia.EstadoOrden.FINALIZADO);
        when(ordenAtencionEmergenciaRepo.findByNumeroOrden("OE-1")).thenReturn(Optional.of(orden));

        assertThat(atencionService.verificarEstadoCitaUOrden("OE-1")).isEqualTo("ATENDIDA");
    }

    @Test
    void registrarAtencionConCitaValidaMarcaCitaAtendidaYCreaReceta() {
        HistoriaClinica historia = TestFixtures.historia(1L);
        Trabajador medico = TestFixtures.medico(10L);
        Medicamento medicamento = TestFixtures.medicamento(30L, 20);
        var turno = TestFixtures.turno(1L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                java.time.LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(12, 0));
        Cita cita = TestFixtures.cita(1L, historia, medico, turno, LocalDateTime.now());
        AtencionMedicaRequestDTO request = TestFixtures.atencionRequest(cita.getNumeroCita());
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(historia));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(medico));
        when(citaRepo.findByNumeroCita(cita.getNumeroCita())).thenReturn(Optional.of(cita));
        when(atencionRepo.save(any(AtencionMedica.class))).thenAnswer(invocation -> {
            AtencionMedica atencion = invocation.getArgument(0);
            atencion.setId(55L);
            return atencion;
        });
        when(medicamentoRepo.findById(30L)).thenReturn(Optional.of(medicamento));

        Long id = atencionService.registrarAtencion(request);

        assertThat(id).isEqualTo(55L);
        assertThat(cita.getEstado()).isEqualTo(Cita.EstadoCita.ATENDIDA);
        ArgumentCaptor<Receta> recetaCaptor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepo).save(recetaCaptor.capture());
        assertThat(recetaCaptor.getValue().getDetalles()).hasSize(1);
        assertThat(recetaCaptor.getValue().getEstado()).isEqualTo(Receta.EstadoReceta.EMITIDA);
    }

    @Test
    void registrarAtencionConMedicoSinRolMedicoLanzaIllegalArgumentException() {
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(TestFixtures.trabajador(10L, "admin", "ADMINISTRADOR", true)));

        assertThatThrownBy(() -> atencionService.registrarAtencion(TestFixtures.atencionRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol MEDICO");
    }

    @Test
    void registrarAtencionConCitaDeOtraHistoriaLanzaIllegalArgumentException() {
        HistoriaClinica historia = TestFixtures.historia(1L);
        HistoriaClinica otraHistoria = TestFixtures.historia(2L);
        Trabajador medico = TestFixtures.medico(10L);
        var turno = TestFixtures.turno(1L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                java.time.LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(12, 0));
        Cita cita = TestFixtures.cita(1L, otraHistoria, medico, turno, LocalDateTime.now());
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(historia));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(medico));
        when(citaRepo.findByNumeroCita(cita.getNumeroCita())).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> atencionService.registrarAtencion(TestFixtures.atencionRequest(cita.getNumeroCita())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void obtenerCitasDisponiblesCombinaCitasConfirmadasYOrdenesPendientes() {
        HistoriaClinica historia = TestFixtures.historia(1L);
        Trabajador medico = TestFixtures.medico(10L);
        var turno = TestFixtures.turno(1L, medico,
                TestFixtures.consultorio(1L, TestFixtures.especialidad(10L, "Medicina General")),
                java.time.LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(12, 0));
        Cita cita = TestFixtures.cita(1L, historia, medico, turno, LocalDateTime.now());
        OrdenAtencionEmergencia orden = TestFixtures.ordenEmergencia(1L, historia, medico);
        when(citaRepo.findByHistoriaClinicaIdAndEstado(1L, Cita.EstadoCita.CONFIRMADA)).thenReturn(List.of(cita));
        when(ordenAtencionEmergenciaRepo.findByHistoriaClinicaIdAndEstado(1L, OrdenAtencionEmergencia.EstadoOrden.PENDIENTE))
                .thenReturn(List.of(orden));

        var response = atencionService.obtenerCitasDisponibles(1L);

        assertThat(response).hasSize(2);
        assertThat(response).extracting("tipo").containsExactly("CITA", "EMERGENCIA");
    }

    @Test
    void registrarAtencionConHistoriaInexistenteLanzaRecursoNoEncontrado() {
        when(historiaRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> atencionService.registrarAtencion(TestFixtures.atencionRequest(null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
