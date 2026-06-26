package com.clinica.services;

import com.clinica.dtos.RecetaRequestDTO;
import com.clinica.dtos.RecetaResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.AtencionMedica;
import com.clinica.model.entities.Medicamento;
import com.clinica.model.entities.OrdenEntrega;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Receta;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.AtencionMedicaRepository;
import com.clinica.model.repositories.MedicamentoRepository;
import com.clinica.model.repositories.OrdenEntregaRepository;
import com.clinica.model.repositories.PacienteRepository;
import com.clinica.model.repositories.RecetaRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    @Mock
    RecetaRepository recetaRepo;

    @Mock
    MedicamentoRepository medicamentoRepo;

    @Mock
    AtencionMedicaRepository atencionMedicaRepo;

    @Mock
    TrabajadorRepository trabajadorRepo;

    @Mock
    PacienteRepository pacienteRepo;

    @Mock
    OrdenEntregaRepository ordenEntregaRepo;

    @Mock
    DeudaService deudaService;

    @InjectMocks
    RecetaService recetaService;

    @AfterEach
    void tearDown() {
        TestFixtures.limpiarAutenticacion();
    }

    @Test
    void buscarConTerminoVacioLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> recetaService.buscar(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("receta");
    }

    @Test
    void buscarSinResultadosLanzaRecursoNoEncontrado() {
        when(recetaRepo.buscarPorNumeroRecetaODni("REC-1")).thenReturn(List.of());

        assertThatThrownBy(() -> recetaService.buscar(" REC-1 "))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("criterio");
    }

    @Test
    void registrarCreaNumeroCorrelativoYDetallesConCantidadPendiente() {
        RecetaRequestDTO dto = TestFixtures.recetaRequest();
        AtencionMedica atencion = AtencionMedica.builder().id(1L).build();
        Trabajador medico = TestFixtures.medico(10L);
        Paciente paciente = TestFixtures.paciente(20L);
        Medicamento medicamento = TestFixtures.medicamento(30L, 20);
        Receta ultima = Receta.builder().id(9L).numeroReceta("REC-000009").build();
        when(atencionMedicaRepo.findById(1L)).thenReturn(Optional.of(atencion));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(medico));
        when(pacienteRepo.findById(20L)).thenReturn(Optional.of(paciente));
        when(medicamentoRepo.findById(30L)).thenReturn(Optional.of(medicamento));
        when(recetaRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.of(ultima));
        when(recetaRepo.save(any(Receta.class))).thenAnswer(invocation -> {
            Receta receta = invocation.getArgument(0);
            receta.setId(100L);
            return receta;
        });

        RecetaResponseDTO response = recetaService.registrar(dto);

        assertThat(response.getNumeroReceta()).isEqualTo("REC-000010");
        assertThat(response.getEstado()).isEqualTo("EMITIDA");
        assertThat(response.getDetalles()).hasSize(1);
        assertThat(response.getDetalles().get(0).getCantidadDespachada()).isZero();
    }

    @Test
    void registrarConMedicamentoInexistenteLanzaRecursoNoEncontrado() {
        RecetaRequestDTO dto = TestFixtures.recetaRequest();
        when(atencionMedicaRepo.findById(1L)).thenReturn(Optional.of(AtencionMedica.builder().id(1L).build()));
        when(trabajadorRepo.findById(10L)).thenReturn(Optional.of(TestFixtures.medico(10L)));
        when(pacienteRepo.findById(20L)).thenReturn(Optional.of(TestFixtures.paciente(20L)));
        when(recetaRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(medicamentoRepo.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaService.registrar(dto))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Medicamento");
    }

    @Test
    void despacharRecetaEmitidaDescuentaStockCreaOrdenYActualizaEstado() {
        TestFixtures.autenticarComo("tecnico");
        Medicamento medicamento = TestFixtures.medicamento(30L, 10);
        Receta receta = TestFixtures.receta(1L, Receta.EstadoReceta.EMITIDA, medicamento, 3);
        Trabajador tecnico = TestFixtures.trabajador(50L, "tecnico", "FARMACIA", true);
        when(recetaRepo.findById(1L)).thenReturn(Optional.of(receta));
        when(trabajadorRepo.findByUsername("tecnico")).thenReturn(Optional.of(tecnico));
        when(ordenEntregaRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.of(OrdenEntrega.builder()
                .id(10L)
                .numeroOrden("ENT-000010")
                .build()));
        when(deudaService.recetaEstaPagada(receta)).thenReturn(true);
        when(ordenEntregaRepo.save(any(OrdenEntrega.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recetaRepo.save(receta)).thenReturn(receta);

        RecetaResponseDTO response = recetaService.despachar(1L);

        assertThat(medicamento.getStockActual()).isEqualTo(7);
        assertThat(receta.getEstado()).isEqualTo(Receta.EstadoReceta.DESPACHADA);
        assertThat(response.getEstado()).isEqualTo("DESPACHADA");
        ArgumentCaptor<OrdenEntrega> ordenCaptor = ArgumentCaptor.forClass(OrdenEntrega.class);
        verify(ordenEntregaRepo).save(ordenCaptor.capture());
        assertThat(ordenCaptor.getValue().getNumeroOrden()).isEqualTo("ENT-000011");
        assertThat(ordenCaptor.getValue().getDetalles()).hasSize(1);
    }

    @Test
    void despacharRecetaNoEmitidaLanzaIllegalStateException() {
        Receta receta = TestFixtures.receta(1L, Receta.EstadoReceta.DESPACHADA, TestFixtures.medicamento(30L, 10), 3);
        when(recetaRepo.findById(1L)).thenReturn(Optional.of(receta));

        assertThatThrownBy(() -> recetaService.despachar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solo se pueden despachar");
    }

    @Test
    void despacharSinStockSuficienteLanzaIllegalStateException() {
        TestFixtures.autenticarComo("tecnico");
        Medicamento medicamento = TestFixtures.medicamento(30L, 2);
        Receta receta = TestFixtures.receta(1L, Receta.EstadoReceta.EMITIDA, medicamento, 3);
        when(recetaRepo.findById(1L)).thenReturn(Optional.of(receta));
        when(trabajadorRepo.findByUsername("tecnico")).thenReturn(Optional.of(TestFixtures.trabajador(50L, "tecnico", "FARMACIA", true)));
        when(ordenEntregaRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(deudaService.recetaEstaPagada(receta)).thenReturn(true);

        assertThatThrownBy(() -> recetaService.despachar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void despacharRecetaNoPagadaLanzaIllegalStateException() {
        TestFixtures.autenticarComo("tecnico");
        Receta receta = TestFixtures.receta(1L, Receta.EstadoReceta.EMITIDA, TestFixtures.medicamento(30L, 10), 3);
        when(recetaRepo.findById(1L)).thenReturn(Optional.of(receta));
        when(trabajadorRepo.findByUsername("tecnico")).thenReturn(Optional.of(TestFixtures.trabajador(50L, "tecnico", "FARMACIA", true)));
        when(deudaService.recetaEstaPagada(receta)).thenReturn(false);

        assertThatThrownBy(() -> recetaService.despachar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelar en caja");
    }

    @Test
    void despacharSinTecnicoAutenticadoEnBdLanzaIllegalStateException() {
        TestFixtures.autenticarComo("tecnico");
        Receta receta = TestFixtures.receta(1L, Receta.EstadoReceta.EMITIDA, TestFixtures.medicamento(30L, 10), 3);
        when(recetaRepo.findById(1L)).thenReturn(Optional.of(receta));
        when(trabajadorRepo.findByUsername("tecnico")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaService.despachar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base de datos");
    }
}
