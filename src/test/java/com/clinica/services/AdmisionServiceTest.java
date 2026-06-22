package com.clinica.services;

import com.clinica.dtos.AbrirHistoriaClinicaRequestDTO;
import com.clinica.dtos.GenerarOrdenEmergenciaRequestDTO;
import com.clinica.dtos.HistoriaClinicaResponseDTO;
import com.clinica.model.entities.HistoriaClinica;
import com.clinica.model.entities.OrdenAtencionEmergencia;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.HistoriaClinicaRepository;
import com.clinica.model.repositories.OrdenAtencionEmergenciaRepository;
import com.clinica.model.repositories.PacienteRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmisionServiceTest {

    @Mock
    HistoriaClinicaRepository historiaRepo;

    @Mock
    OrdenAtencionEmergenciaRepository ordenRepo;

    @Mock
    TrabajadorRepository trabajadorRepo;

    @Mock
    PacienteRepository pacienteRepo;

    @InjectMocks
    AdmisionService admisionService;

    @AfterEach
    void tearDown() {
        TestFixtures.limpiarAutenticacion();
    }

    @Test
    void buscarPorDniNormalizaDocumentoAntesDeConsultar() {
        HistoriaClinica historia = TestFixtures.historia(1L);
        when(historiaRepo.findByDniPaciente("ABC123")).thenReturn(Optional.of(historia));

        HistoriaClinicaResponseDTO response = admisionService.buscarPorDni(" abc123 ");

        assertThat(response.getDniPaciente()).isEqualTo(historia.getDniPaciente());
        verify(historiaRepo).findByDniPaciente("ABC123");
    }

    @Test
    void buscarPorDniInvalidoLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> admisionService.buscarPorDni("1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no debe superar 9");
    }

    @Test
    void buscarPorNumeroHistoriaVacioLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> admisionService.buscarPorNumeroHistoria(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void abrirHistoriaConDniExistenteLanzaIllegalStateException() {
        when(historiaRepo.existsByDniPaciente("12345678")).thenReturn(true);

        assertThatThrownBy(() -> admisionService.abrirHistoria(TestFixtures.abrirHistoriaRequest("12345678")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void abrirHistoriaCreaPacienteNuevoYRedirectDeAdmision() {
        TestFixtures.autenticarComo("admision");
        AbrirHistoriaClinicaRequestDTO dto = TestFixtures.abrirHistoriaRequest(" 12345678 ");
        Trabajador autor = TestFixtures.trabajador(99L, "admision", "ADMISION", true);
        when(historiaRepo.existsByDniPaciente("12345678")).thenReturn(false);
        when(pacienteRepo.findByDni("12345678")).thenReturn(Optional.empty());
        when(trabajadorRepo.findByUsername("admision")).thenReturn(Optional.of(autor));
        when(historiaRepo.existsByNumeroHistoria(any())).thenReturn(false);
        when(pacienteRepo.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente paciente = invocation.getArgument(0);
            paciente.setId(10L);
            return paciente;
        });
        when(historiaRepo.save(any(HistoriaClinica.class))).thenAnswer(invocation -> {
            HistoriaClinica historia = invocation.getArgument(0);
            historia.setId(20L);
            return historia;
        });

        HistoriaClinicaResponseDTO response = admisionService.abrirHistoria(dto);

        assertThat(response.isNuevaHistoria()).isTrue();
        assertThat(response.getRedirectUrl()).contains("/admision/emergencia");
        ArgumentCaptor<HistoriaClinica> captor = ArgumentCaptor.forClass(HistoriaClinica.class);
        verify(historiaRepo).save(captor.capture());
        assertThat(captor.getValue().getDniPaciente()).isEqualTo("12345678");
        assertThat(captor.getValue().getCreadoPor()).isSameAs(autor);
    }

    @Test
    void generarOrdenEmergenciaConTrabajadorNoMedicoLanzaIllegalArgumentException() {
        GenerarOrdenEmergenciaRequestDTO dto = new GenerarOrdenEmergenciaRequestDTO();
        dto.setHistoriaClinicaId(1L);
        dto.setMedicoId(2L);
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(trabajadorRepo.findById(2L)).thenReturn(Optional.of(TestFixtures.trabajador(2L, "admin", "ADMINISTRADOR", true)));

        assertThatThrownBy(() -> admisionService.generarOrdenEmergencia(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol de");
    }

    @Test
    void generarOrdenEmergenciaConMedicoInactivoLanzaIllegalStateException() {
        GenerarOrdenEmergenciaRequestDTO dto = new GenerarOrdenEmergenciaRequestDTO();
        dto.setHistoriaClinicaId(1L);
        dto.setMedicoId(10L);
        Trabajador medico = TestFixtures.medico(10L);
        medico.setActivo(false);
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(TestFixtures.historia(1L)));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> admisionService.generarOrdenEmergencia(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void generarOrdenEmergenciaValidaGuardaOrdenPendiente() {
        TestFixtures.autenticarComo("admision");
        GenerarOrdenEmergenciaRequestDTO dto = new GenerarOrdenEmergenciaRequestDTO();
        dto.setHistoriaClinicaId(1L);
        dto.setMedicoId(10L);
        dto.setMotivo("Dolor agudo");
        HistoriaClinica historia = TestFixtures.historia(1L);
        Trabajador medico = TestFixtures.medico(10L);
        Trabajador autor = TestFixtures.trabajador(99L, "admision", "ADMISION", true);
        when(historiaRepo.findById(1L)).thenReturn(Optional.of(historia));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(medico));
        when(trabajadorRepo.findByUsername("admision")).thenReturn(Optional.of(autor));
        when(ordenRepo.existsByNumeroOrden(any())).thenReturn(false);
        when(ordenRepo.save(any(OrdenAtencionEmergencia.class))).thenAnswer(invocation -> {
            OrdenAtencionEmergencia orden = invocation.getArgument(0);
            orden.setId(30L);
            return orden;
        });

        var response = admisionService.generarOrdenEmergencia(dto);

        assertThat(response.getEstado()).isEqualTo("PENDIENTE");
        assertThat(response.getMotivo()).isEqualTo("Dolor agudo");
        verify(ordenRepo).save(any(OrdenAtencionEmergencia.class));
    }
}
