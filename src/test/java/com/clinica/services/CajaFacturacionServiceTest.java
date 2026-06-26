package com.clinica.services;

import com.clinica.dtos.PagoRequestDTO;
import com.clinica.model.entities.AperturaCaja;
import com.clinica.model.entities.AsignacionCaja;
import com.clinica.model.entities.Caja;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.Comprobante;
import com.clinica.model.entities.OrdenServicio;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Pago;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.SerieComprobante;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.AperturaCajaRepository;
import com.clinica.model.repositories.AsignacionCajaRepository;
import com.clinica.model.repositories.CajaRepository;
import com.clinica.model.repositories.ComprobanteRepository;
import com.clinica.model.repositories.DetalleComprobanteRepository;
import com.clinica.model.repositories.OrdenServicioRepository;
import com.clinica.model.repositories.PagoRepository;
import com.clinica.model.repositories.SerieComprobanteRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaFacturacionServiceTest {

    @Mock
    CajaRepository cajaRepository;

    @Mock
    AsignacionCajaRepository asignacionCajaRepository;

    @Mock
    AperturaCajaRepository aperturaCajaRepository;

    @Mock
    OrdenServicioRepository ordenServicioRepository;

    @Mock
    PagoRepository pagoRepository;

    @Mock
    ComprobanteRepository comprobanteRepository;

    @Mock
    DetalleComprobanteRepository detalleComprobanteRepository;

    @Mock
    SerieComprobanteRepository serieComprobanteRepository;

    @Mock
    TrabajadorRepository trabajadorRepository;

    @Mock
    DeudaService deudaService;

    @InjectMocks
    CajaFacturacionService cajaService;

    @AfterEach
    void tearDown() {
        TestFixtures.limpiarAutenticacion();
    }

    @Test
    void emitirComprobanteSinCajaAbiertaLanzaIllegalStateException() {
        TestFixtures.autenticarComo("cajero");
        Trabajador cajero = cajero(1L);
        when(trabajadorRepository.findByUsername("cajero")).thenReturn(Optional.of(cajero));
        when(aperturaCajaRepository.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(
                1L, AperturaCaja.EstadoCaja.ABIERTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.emitirComprobante(pagoRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("caja abierta");
    }

    @Test
    void emitirComprobantePagaDeudaDeCitaYConsumeCorrelativo() {
        TestFixtures.autenticarComo("cajero");
        Trabajador cajero = cajero(1L);
        Caja caja = Caja.builder().id(2L).nombre("Caja 1").activo(true).build();
        AperturaCaja apertura = AperturaCaja.builder()
                .id(3L)
                .caja(caja)
                .cajero(cajero)
                .montoApertura(new BigDecimal("50.00"))
                .estado(AperturaCaja.EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now())
                .build();
        SerieComprobante boleta = SerieComprobante.builder()
                .id(4L)
                .serie("B001")
                .tipoComprobante(SerieComprobante.TipoComprobante.BOLETA)
                .correlativoActual(0)
                .activo(true)
                .build();
        AsignacionCaja asignacion = AsignacionCaja.builder()
                .id(5L)
                .cajero(cajero)
                .caja(caja)
                .serieBoleta(boleta)
                .serieFactura(SerieComprobante.builder()
                        .id(6L)
                        .serie("F001")
                        .tipoComprobante(SerieComprobante.TipoComprobante.FACTURA)
                        .correlativoActual(0)
                        .activo(true)
                        .build())
                .activo(true)
                .build();
        Paciente paciente = TestFixtures.paciente(7L);
        Cita cita = Cita.builder()
                .id(8L)
                .numeroCita("CT-1")
                .paciente(paciente)
                .estado(Cita.EstadoCita.PROGRAMADA)
                .build();
        OrdenServicio deuda = OrdenServicio.builder()
                .id(9L)
                .numeroOrden("CIT-1")
                .paciente(paciente)
                .cita(cita)
                .estado(OrdenServicio.EstadoOrden.PENDIENTE)
                .total(new BigDecimal("80.00"))
                .build();

        when(trabajadorRepository.findByUsername("cajero")).thenReturn(Optional.of(cajero));
        when(aperturaCajaRepository.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(
                1L, AperturaCaja.EstadoCaja.ABIERTA)).thenReturn(Optional.of(apertura));
        when(ordenServicioRepository.findByIdIn(anyCollection())).thenReturn(List.of(deuda));
        when(asignacionCajaRepository.findFirstByCajeroIdAndActivoTrueOrderByIdDesc(1L))
                .thenReturn(Optional.of(asignacion));
        when(comprobanteRepository.save(any(Comprobante.class))).thenAnswer(invocation -> {
            Comprobante comprobante = invocation.getArgument(0);
            comprobante.setId(10L);
            return comprobante;
        });
        when(pagoRepository.findByComprobanteId(10L)).thenReturn(List.of(Pago.builder()
                .ordenServicio(deuda)
                .metodoPago(Pago.MetodoPago.EFECTIVO)
                .build()));

        var response = cajaService.emitirComprobante(pagoRequest());

        assertThat(response.getNumeroCompleto()).isEqualTo("B001-000001");
        assertThat(boleta.getCorrelativoActual()).isEqualTo(1);
        assertThat(deuda.getEstado()).isEqualTo(OrdenServicio.EstadoOrden.PAGADA);
        assertThat(cita.getEstado()).isEqualTo(Cita.EstadoCita.CONFIRMADA);
        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pagoCaptor.capture());
        assertThat(pagoCaptor.getValue().getMonto()).isEqualByComparingTo("80.00");
    }

    private PagoRequestDTO pagoRequest() {
        PagoRequestDTO request = new PagoRequestDTO();
        request.setDeudaIds(List.of(9L));
        request.setMetodoPago(Pago.MetodoPago.EFECTIVO);
        request.setTipoComprobante(SerieComprobante.TipoComprobante.BOLETA);
        return request;
    }

    private Trabajador cajero(Long id) {
        return Trabajador.builder()
                .id(id)
                .username("cajero")
                .nombreCompleto("Cajero Demo")
                .activo(true)
                .rol(Rol.builder().id(3L).nombre("CAJERO").build())
                .build();
    }
}
