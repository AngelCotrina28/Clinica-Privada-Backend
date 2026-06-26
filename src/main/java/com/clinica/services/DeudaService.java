package com.clinica.services;

import com.clinica.dtos.DeudaResponseDTO;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.DetalleReceta;
import com.clinica.model.entities.OrdenAtencionEmergencia;
import com.clinica.model.entities.OrdenServicio;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Receta;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.OrdenServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeudaService {

    public static final String CONCEPTO_CITA = "GASTOS_CITA";
    public static final String CONCEPTO_EMERGENCIA = "GASTOS_EMERGENCIA";
    public static final String CONCEPTO_MEDICINA = "GASTOS_MEDICINA";

    private static final BigDecimal MONTO_BASE_CITA = new BigDecimal("80.00");
    private static final BigDecimal MONTO_BASE_EMERGENCIA = new BigDecimal("150.00");
    private static final DateTimeFormatter FECHA_ORDEN = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrdenServicioRepository ordenServicioRepository;

    @Transactional(readOnly = true)
    public List<DeudaResponseDTO> listarPendientesPorDni(String dni, String concepto) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar el DNI del paciente.");
        }

        String conceptoNormalizado = normalizarConcepto(concepto);
        return ordenServicioRepository
                .findByPacienteDniAndEstadoOrderByCreatedAtDesc(dni.trim(), OrdenServicio.EstadoOrden.PENDIENTE)
                .stream()
                .filter(deuda -> conceptoNormalizado == null || conceptoNormalizado.equals(inferirConcepto(deuda)))
                .map(this::map)
                .toList();
    }

    @Transactional
    public OrdenServicio asegurarDeudaCita(Cita cita, Trabajador creador) {
        if (cita == null || cita.getId() == null) {
            throw new IllegalArgumentException("La cita es obligatoria para generar la deuda.");
        }

        return ordenServicioRepository.findFirstByCitaIdOrderByIdDesc(cita.getId())
                .orElseGet(() -> ordenServicioRepository.save(OrdenServicio.builder()
                        .numeroOrden(generarNumeroOrden("CIT"))
                        .paciente(cita.getPaciente())
                        .cita(cita)
                        .estado(OrdenServicio.EstadoOrden.PENDIENTE)
                        .subtotal(MONTO_BASE_CITA)
                        .igv(BigDecimal.ZERO)
                        .descuento(BigDecimal.ZERO)
                        .total(MONTO_BASE_CITA)
                        .observaciones(CONCEPTO_CITA + "|CITA:" + cita.getNumeroCita())
                        .creadoPor(creador)
                        .build()));
    }

    @Transactional
    public OrdenServicio asegurarDeudaEmergencia(OrdenAtencionEmergencia orden, Trabajador creador) {
        if (orden == null || orden.getId() == null) {
            throw new IllegalArgumentException("La orden de emergencia es obligatoria para generar la deuda.");
        }

        return ordenServicioRepository.findFirstByOrdenEmergenciaIdOrderByIdDesc(orden.getId())
                .orElseGet(() -> ordenServicioRepository.save(OrdenServicio.builder()
                        .numeroOrden(generarNumeroOrden("EMG"))
                        .paciente(orden.getHistoriaClinica().getPaciente())
                        .ordenEmergencia(orden)
                        .estado(OrdenServicio.EstadoOrden.PENDIENTE)
                        .subtotal(MONTO_BASE_EMERGENCIA)
                        .igv(BigDecimal.ZERO)
                        .descuento(BigDecimal.ZERO)
                        .total(MONTO_BASE_EMERGENCIA)
                        .observaciones(CONCEPTO_EMERGENCIA + "|EMERGENCIA:" + orden.getNumeroOrden())
                        .creadoPor(creador)
                        .build()));
    }

    @Transactional
    public OrdenServicio asegurarDeudaMedicina(Receta receta, Trabajador creador) {
        if (receta == null || receta.getId() == null) {
            throw new IllegalArgumentException("La receta es obligatoria para generar la deuda de medicina.");
        }

        return buscarDeudaMedicina(receta)
                .orElseGet(() -> {
                    BigDecimal total = calcularTotalMedicina(receta);
                    return ordenServicioRepository.save(OrdenServicio.builder()
                            .numeroOrden(generarNumeroOrden("MED"))
                            .paciente(receta.getPaciente())
                            .estado(OrdenServicio.EstadoOrden.PENDIENTE)
                            .subtotal(total)
                            .igv(BigDecimal.ZERO)
                            .descuento(BigDecimal.ZERO)
                            .total(total)
                            .observaciones(CONCEPTO_MEDICINA + "|RECETA:" + receta.getNumeroReceta())
                            .creadoPor(creador)
                            .build());
                });
    }

    @Transactional(readOnly = true)
    public boolean recetaEstaPagada(Receta receta) {
        return buscarDeudaMedicina(receta)
                .map(deuda -> deuda.getEstado() == OrdenServicio.EstadoOrden.PAGADA)
                .orElse(false);
    }

    public DeudaResponseDTO map(OrdenServicio deuda) {
        Paciente paciente = deuda.getPaciente();
        String concepto = inferirConcepto(deuda);

        return DeudaResponseDTO.builder()
                .id(deuda.getId())
                .numeroOrden(deuda.getNumeroOrden())
                .concepto(concepto)
                .conceptoLabel(labelConcepto(concepto))
                .monto(deuda.getTotal())
                .estado(deuda.getEstado().name())
                .pacienteId(paciente != null ? paciente.getId() : null)
                .pacienteDni(paciente != null ? paciente.getDni() : null)
                .pacienteNombre(paciente != null ? paciente.getNombreCompleto() : null)
                .origenCodigo(obtenerCodigoOrigen(deuda))
                .fechaGeneracion(deuda.getCreatedAt())
                .build();
    }

    public String inferirConcepto(OrdenServicio deuda) {
        String observaciones = deuda.getObservaciones() == null ? "" : deuda.getObservaciones().toUpperCase(Locale.ROOT);
        if (observaciones.contains(CONCEPTO_MEDICINA) || observaciones.contains("RECETA:")) {
            return CONCEPTO_MEDICINA;
        }
        if (deuda.getOrdenEmergencia() != null || observaciones.contains(CONCEPTO_EMERGENCIA)) {
            return CONCEPTO_EMERGENCIA;
        }
        return CONCEPTO_CITA;
    }

    public String labelConcepto(String concepto) {
        return switch (concepto) {
            case CONCEPTO_EMERGENCIA -> "Gastos de emergencia";
            case CONCEPTO_MEDICINA -> "Gastos de medicina";
            default -> "Gastos de cita";
        };
    }

    private java.util.Optional<OrdenServicio> buscarDeudaMedicina(Receta receta) {
        if (receta == null || receta.getPaciente() == null || receta.getNumeroReceta() == null) {
            return java.util.Optional.empty();
        }

        String marcador = "RECETA:" + receta.getNumeroReceta();
        return ordenServicioRepository
                .findByPacienteIdAndObservacionesContainingIgnoreCaseOrderByIdDesc(
                        receta.getPaciente().getId(), marcador)
                .stream()
                .filter(deuda -> deuda.getEstado() != OrdenServicio.EstadoOrden.ANULADA)
                .findFirst();
    }

    private BigDecimal calcularTotalMedicina(Receta receta) {
        BigDecimal total = receta.getDetalles() == null
                ? BigDecimal.ZERO
                : receta.getDetalles().stream()
                        .filter(Objects::nonNull)
                        .map(this::subtotalDetalle)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La receta no tiene un monto de medicina valido para cobrar.");
        }
        return total;
    }

    private BigDecimal subtotalDetalle(DetalleReceta detalle) {
        if (detalle.getMedicamento() == null || detalle.getMedicamento().getPrecioUnitario() == null) {
            return BigDecimal.ZERO;
        }
        int cantidad = detalle.getCantidadPrescrita() == null ? 0 : detalle.getCantidadPrescrita();
        return detalle.getMedicamento().getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad));
    }

    private String obtenerCodigoOrigen(OrdenServicio deuda) {
        if (deuda.getCita() != null) {
            return deuda.getCita().getNumeroCita();
        }
        if (deuda.getOrdenEmergencia() != null) {
            return deuda.getOrdenEmergencia().getNumeroOrden();
        }
        String observaciones = deuda.getObservaciones();
        if (observaciones != null && observaciones.contains("RECETA:")) {
            return observaciones.substring(observaciones.indexOf("RECETA:") + "RECETA:".length()).trim();
        }
        return deuda.getNumeroOrden();
    }

    private String normalizarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank() || "TODOS".equalsIgnoreCase(concepto)) {
            return null;
        }
        return concepto.trim().toUpperCase(Locale.ROOT);
    }

    private String generarNumeroOrden(String prefijo) {
        String numero;
        do {
            int correlativo = (int) (1000 + Math.random() * 9000);
            numero = prefijo + "-" + LocalDate.now().format(FECHA_ORDEN) + "-" + correlativo;
        } while (ordenServicioRepository.existsByNumeroOrden(numero));
        return numero;
    }
}
