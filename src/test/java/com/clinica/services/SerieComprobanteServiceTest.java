package com.clinica.services;

import com.clinica.dtos.SerieComprobanteRequestDTO;
import com.clinica.dtos.SerieComprobanteResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.SerieComprobante;
import com.clinica.model.repositories.SerieComprobanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
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
class SerieComprobanteServiceTest {

    @Mock
    SerieComprobanteRepository serieRepository;

    @InjectMocks
    SerieComprobanteService serieService;

    @Test
    void listarTodosSoloExponeBoletasYFacturasParaHu21() {
        SerieComprobante boleta = SerieComprobante.builder()
                .id(1L)
                .tipoComprobante(SerieComprobante.TipoComprobante.BOLETA)
                .serie("B001")
                .correlativoActual(0)
                .activo(true)
                .build();
        SerieComprobante notaCredito = SerieComprobante.builder()
                .id(2L)
                .tipoComprobante(SerieComprobante.TipoComprobante.NOTA_CREDITO)
                .serie("BC01")
                .correlativoActual(0)
                .activo(true)
                .build();
        SerieComprobante factura = SerieComprobante.builder()
                .id(3L)
                .tipoComprobante(SerieComprobante.TipoComprobante.FACTURA)
                .serie("F001")
                .correlativoActual(0)
                .activo(true)
                .build();
        when(serieRepository.findAll(Sort.by("tipoComprobante", "serie")))
                .thenReturn(List.of(boleta, notaCredito, factura));

        List<SerieComprobanteResponseDTO> response = serieService.listarTodos();

        assertThat(response)
                .extracting(SerieComprobanteResponseDTO::getTipoComprobante)
                .containsExactly("BOLETA", "FACTURA");
    }

    @Test
    void crearBoletaNormalizaPrefijoYGuardaCorrelativoAnteriorAlInicial() {
        SerieComprobanteRequestDTO request = request(SerieComprobante.TipoComprobante.BOLETA, " b001 ", 1, true);
        when(serieRepository.existsBySerieIgnoreCaseAndActivoTrue("B001")).thenReturn(false);
        when(serieRepository.existsByTipoComprobanteAndSerieIgnoreCase(SerieComprobante.TipoComprobante.BOLETA, "B001"))
                .thenReturn(false);
        when(serieRepository.save(any(SerieComprobante.class))).thenAnswer(invocation -> {
            SerieComprobante serie = invocation.getArgument(0);
            serie.setId(10L);
            return serie;
        });

        SerieComprobanteResponseDTO response = serieService.crear(request);

        assertThat(response.getPrefijo()).isEqualTo("B001");
        assertThat(response.getCorrelativoActual()).isZero();
        assertThat(response.getSiguienteNumero()).isEqualTo("B001-000001");
        ArgumentCaptor<SerieComprobante> captor = ArgumentCaptor.forClass(SerieComprobante.class);
        verify(serieRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrelativoActual()).isZero();
    }

    @Test
    void crearConPrefijoActivoDuplicadoLanzaIllegalStateException() {
        SerieComprobanteRequestDTO request = request(SerieComprobante.TipoComprobante.BOLETA, "B001", 1, true);
        when(serieRepository.existsBySerieIgnoreCaseAndActivoTrue("B001")).thenReturn(true);

        assertThatThrownBy(() -> serieService.crear(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serie activa");
    }

    @Test
    void crearFacturaConPrefijoDeBoletaLanzaIllegalArgumentException() {
        SerieComprobanteRequestDTO request = request(SerieComprobante.TipoComprobante.FACTURA, "B001", 1, true);

        assertThatThrownBy(() -> serieService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("factura");
    }

    @Test
    void cambiarEstadoDeInactivaValidaPrefijoActivoAntesDeReactivar() {
        SerieComprobante serie = SerieComprobante.builder()
                .id(1L)
                .tipoComprobante(SerieComprobante.TipoComprobante.BOLETA)
                .serie("B001")
                .correlativoActual(0)
                .activo(false)
                .build();
        when(serieRepository.findById(1L)).thenReturn(Optional.of(serie));
        when(serieRepository.existsBySerieIgnoreCaseAndActivoTrueAndIdNot("B001", 1L)).thenReturn(false);

        serieService.cambiarEstado(1L);

        assertThat(serie.isActivo()).isTrue();
        verify(serieRepository).save(serie);
    }

    @Test
    void actualizarSerieInexistenteLanzaRecursoNoEncontrado() {
        when(serieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serieService.actualizar(99L,
                request(SerieComprobante.TipoComprobante.BOLETA, "B001", 1, true)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private SerieComprobanteRequestDTO request(
            SerieComprobante.TipoComprobante tipo,
            String prefijo,
            int numeroInicial,
            boolean activo) {
        SerieComprobanteRequestDTO request = new SerieComprobanteRequestDTO();
        request.setTipoComprobante(tipo);
        request.setPrefijo(prefijo);
        request.setNumeroInicial(numeroInicial);
        request.setActivo(activo);
        return request;
    }
}
