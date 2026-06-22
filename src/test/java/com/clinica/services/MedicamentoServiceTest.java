package com.clinica.services;

import com.clinica.dtos.MedicamentoRequestDTO;
import com.clinica.dtos.MedicamentoResponseDTO;
import com.clinica.exceptions.MedicamentoInactivoException;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.mappers.MedicamentoMapper;
import com.clinica.model.entities.CategoriaMedicamento;
import com.clinica.model.entities.HistorialMedicamento;
import com.clinica.model.entities.Medicamento;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.CategoriaMedicamentoRepository;
import com.clinica.model.repositories.HistorialMedicamentoRepository;
import com.clinica.model.repositories.MedicamentoRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import com.clinica.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

    @Mock
    MedicamentoRepository medicamentoRepo;

    @Mock
    CategoriaMedicamentoRepository categoriaRepo;

    @Mock
    HistorialMedicamentoRepository historialRepo;

    @Mock
    TrabajadorRepository trabajadorRepo;

    @Mock
    MedicamentoMapper mapper;

    @InjectMocks
    MedicamentoService medicamentoService;

    @AfterEach
    void tearDown() {
        TestFixtures.limpiarAutenticacion();
    }

    @Test
    void registrarCreaMedicamentoActivoConHistorialDeCreacion() {
        TestFixtures.autenticarComo("farmacia");
        MedicamentoRequestDTO dto = TestFixtures.medicamentoRequest();
        CategoriaMedicamento categoria = TestFixtures.categoria(1);
        Trabajador farmacia = TestFixtures.trabajador(9L, "farmacia", "FARMACIA", true);
        MedicamentoResponseDTO response = MedicamentoResponseDTO.builder().id(1L).nombre("Paracetamol").build();
        when(categoriaRepo.findById(1)).thenReturn(Optional.of(categoria));
        when(trabajadorRepo.findByUsername("farmacia")).thenReturn(Optional.of(farmacia));
        when(medicamentoRepo.save(any(Medicamento.class))).thenAnswer(invocation -> {
            Medicamento medicamento = invocation.getArgument(0);
            medicamento.setId(1L);
            return medicamento;
        });
        when(mapper.toResponse(any(Medicamento.class))).thenReturn(response);

        MedicamentoResponseDTO result = medicamentoService.registrar(dto);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Medicamento> medicamentoCaptor = ArgumentCaptor.forClass(Medicamento.class);
        verify(medicamentoRepo).save(medicamentoCaptor.capture());
        assertThat(medicamentoCaptor.getValue().getCodigo()).startsWith("MED-");
        assertThat(medicamentoCaptor.getValue().isActivo()).isTrue();
        assertThat(medicamentoCaptor.getValue().getCreatedBy()).isEqualTo(farmacia);
        verify(historialRepo).save(any(HistorialMedicamento.class));
    }

    @Test
    void registrarSinTrabajadorAutenticadoLanzaIllegalStateException() {
        when(categoriaRepo.findById(1)).thenReturn(Optional.of(TestFixtures.categoria(1)));

        assertThatThrownBy(() -> medicamentoService.registrar(TestFixtures.medicamentoRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trabajador autenticado");
    }

    @Test
    void registrarConCategoriaInexistenteLanzaRecursoNoEncontrado() {
        when(categoriaRepo.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicamentoService.registrar(TestFixtures.medicamentoRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void editarMedicamentoInactivoNoPermiteCambios() {
        Medicamento medicamento = TestFixtures.medicamento(1L, 20);
        medicamento.setActivo(false);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> medicamentoService.editar(1L, TestFixtures.medicamentoRequest()))
                .isInstanceOf(MedicamentoInactivoException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void agregarStockIncrementaStockYRegistraHistorial() {
        TestFixtures.autenticarComo("farmacia");
        Medicamento medicamento = TestFixtures.medicamento(1L, 10);
        Trabajador farmacia = TestFixtures.trabajador(9L, "farmacia", "FARMACIA", true);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));
        when(trabajadorRepo.findByUsername("farmacia")).thenReturn(Optional.of(farmacia));
        when(medicamentoRepo.save(medicamento)).thenReturn(medicamento);
        when(mapper.toResponse(medicamento)).thenReturn(MedicamentoResponseDTO.builder()
                .id(1L)
                .stockActual(15)
                .build());

        MedicamentoResponseDTO result = medicamentoService.agregarStock(1L, 5);

        assertThat(result.getStockActual()).isEqualTo(15);
        assertThat(medicamento.getStockActual()).isEqualTo(15);
        verify(historialRepo).save(any(HistorialMedicamento.class));
    }

    @Test
    void agregarStockAMedicamentoInactivoLanzaMedicamentoInactivo() {
        Medicamento medicamento = TestFixtures.medicamento(1L, 10);
        medicamento.setActivo(false);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> medicamentoService.agregarStock(1L, 5))
                .isInstanceOf(MedicamentoInactivoException.class);
    }

    @Test
    void inactivarMedicamentoYaInactivoLanzaMedicamentoInactivo() {
        Medicamento medicamento = TestFixtures.medicamento(1L, 10);
        medicamento.setActivo(false);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> medicamentoService.inactivar(1L))
                .isInstanceOf(MedicamentoInactivoException.class)
                .hasMessageContaining("ya se encuentra inactivo");
    }

    @Test
    void activarMedicamentoActivoLanzaIllegalStateException() {
        Medicamento medicamento = TestFixtures.medicamento(1L, 10);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> medicamentoService.activar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra activo");
    }

    @Test
    void editarAuditaCambioDePrecioYNoReseteaStockActual() {
        TestFixtures.autenticarComo("farmacia");
        Medicamento medicamento = TestFixtures.medicamento(1L, 50);
        Trabajador farmacia = TestFixtures.trabajador(9L, "farmacia", "FARMACIA", true);
        MedicamentoRequestDTO dto = TestFixtures.medicamentoRequest();
        dto.setPrecioUnitario(new BigDecimal("6.00"));
        dto.setStockInicial(1);
        when(medicamentoRepo.findById(1L)).thenReturn(Optional.of(medicamento));
        when(trabajadorRepo.findByUsername("farmacia")).thenReturn(Optional.of(farmacia));
        when(categoriaRepo.findById(1)).thenReturn(Optional.of(TestFixtures.categoria(1)));
        when(medicamentoRepo.save(medicamento)).thenReturn(medicamento);
        when(mapper.toResponse(medicamento)).thenReturn(MedicamentoResponseDTO.builder()
                .id(1L)
                .stockActual(50)
                .build());

        MedicamentoResponseDTO result = medicamentoService.editar(1L, dto);

        assertThat(result.getStockActual()).isEqualTo(50);
        assertThat(medicamento.getStockActual()).isEqualTo(50);
        verify(historialRepo).save(any(HistorialMedicamento.class));
    }

    @Test
    void buscarConCampoOrdenNoPermitidoLanzaIllegalArgumentException() {
        assertThatThrownBy(() -> medicamentoService.buscar(null, null, null, true, 0, 20, "DROP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campo de ordenamiento");
    }
}
