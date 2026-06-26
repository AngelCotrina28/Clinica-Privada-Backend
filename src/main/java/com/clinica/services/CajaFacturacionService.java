package com.clinica.services;

import com.clinica.dtos.AnularComprobanteRequestDTO;
import com.clinica.dtos.AperturaCajaRequestDTO;
import com.clinica.dtos.AperturaCajaResponseDTO;
import com.clinica.dtos.AsignacionCajaRequestDTO;
import com.clinica.dtos.AsignacionCajaResponseDTO;
import com.clinica.dtos.CajaResponseDTO;
import com.clinica.dtos.ComprobanteResponseDTO;
import com.clinica.dtos.CuadreCajaRequestDTO;
import com.clinica.dtos.DeudaResponseDTO;
import com.clinica.dtos.PagoRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.AperturaCaja;
import com.clinica.model.entities.AsignacionCaja;
import com.clinica.model.entities.Caja;
import com.clinica.model.entities.Cita;
import com.clinica.model.entities.Comprobante;
import com.clinica.model.entities.DetalleComprobante;
import com.clinica.model.entities.OrdenServicio;
import com.clinica.model.entities.Paciente;
import com.clinica.model.entities.Pago;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CajaFacturacionService {

    private static final int LONGITUD_CORRELATIVO = 6;

    private final CajaRepository cajaRepository;
    private final AsignacionCajaRepository asignacionCajaRepository;
    private final AperturaCajaRepository aperturaCajaRepository;
    private final OrdenServicioRepository ordenServicioRepository;
    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final DetalleComprobanteRepository detalleComprobanteRepository;
    private final SerieComprobanteRepository serieComprobanteRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final DeudaService deudaService;

    @Transactional(readOnly = true)
    public List<CajaResponseDTO> listarCajasActivas() {
        return cajaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(this::mapCaja)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrabajadorResponseDTO> listarCajerosActivos() {
        return trabajadorRepository.findByRolNombreIgnoreCaseAndActivoTrue("CAJERO").stream()
                .map(this::mapTrabajador)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionCajaResponseDTO> listarAsignaciones() {
        return asignacionCajaRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapAsignacion)
                .toList();
    }

    @Transactional
    public AsignacionCajaResponseDTO crearAsignacion(AsignacionCajaRequestDTO request) {
        Trabajador cajero = obtenerCajeroActivo(request.getCajeroId());
        if (asignacionCajaRepository.existsByCajeroIdAndActivoTrue(cajero.getId())) {
            throw new IllegalStateException("El cajero ya tiene una asignacion activa.");
        }

        AsignacionCaja asignacion = construirAsignacion(new AsignacionCaja(), request, cajero);
        return mapAsignacion(asignacionCajaRepository.save(asignacion));
    }

    @Transactional
    public AsignacionCajaResponseDTO actualizarAsignacion(Long id, AsignacionCajaRequestDTO request) {
        AsignacionCaja asignacion = asignacionCajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignacion de caja no encontrada."));
        Trabajador cajero = obtenerCajeroActivo(request.getCajeroId());

        boolean activa = request.getActivo() == null || request.getActivo();
        if (activa && asignacionCajaRepository.existsByCajeroIdAndActivoTrueAndIdNot(cajero.getId(), id)) {
            throw new IllegalStateException("El cajero ya tiene otra asignacion activa.");
        }

        return mapAsignacion(asignacionCajaRepository.save(construirAsignacion(asignacion, request, cajero)));
    }

    @Transactional
    public AsignacionCajaResponseDTO cambiarEstadoAsignacion(Long id, boolean activo) {
        AsignacionCaja asignacion = asignacionCajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignacion de caja no encontrada."));
        if (activo) {
            obtenerCajeroActivo(asignacion.getCajero().getId());
            if (asignacionCajaRepository.existsByCajeroIdAndActivoTrueAndIdNot(asignacion.getCajero().getId(), id)) {
                throw new IllegalStateException("El cajero ya tiene otra asignacion activa.");
            }
        }
        asignacion.setActivo(activo);
        return mapAsignacion(asignacionCajaRepository.save(asignacion));
    }

    @Transactional
    public AperturaCajaResponseDTO abrirCaja(AperturaCajaRequestDTO request) {
        Trabajador cajero = obtenerTrabajadorAutenticado();
        validarRol(cajero, "CAJERO");
        validarActivo(cajero, "El cajero autenticado no esta activo.");

        aperturaCajaRepository
                .findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(cajero.getId(), AperturaCaja.EstadoCaja.ABIERTA)
                .ifPresent(apertura -> {
                    throw new IllegalStateException("Tiene una caja abierta. Debe cuadrarla antes de abrir otro turno.");
                });

        AsignacionCaja asignacion = obtenerAsignacionActiva(cajero.getId());
        BigDecimal montoInicial = request.getMontoInicial();
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto inicial debe ser mayor o igual a 0.00.");
        }

        AperturaCaja apertura = AperturaCaja.builder()
                .caja(asignacion.getCaja())
                .cajero(cajero)
                .montoApertura(montoInicial)
                .estado(AperturaCaja.EstadoCaja.ABIERTA)
                .fechaApertura(LocalDateTime.now())
                .build();

        return mapApertura(aperturaCajaRepository.save(apertura));
    }

    @Transactional(readOnly = true)
    public AperturaCajaResponseDTO obtenerAperturaActual() {
        Trabajador cajero = obtenerTrabajadorAutenticado();
        validarRol(cajero, "CAJERO");
        return aperturaCajaRepository.findFirstByCajeroIdAndEstadoInOrderByFechaAperturaDesc(
                        cajero.getId(),
                        List.of(AperturaCaja.EstadoCaja.ABIERTA, AperturaCaja.EstadoCaja.CUADRADA))
                .map(this::mapApertura)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DeudaResponseDTO> listarDeudasPendientes(String dni, String concepto) {
        return deudaService.listarPendientesPorDni(dni, concepto);
    }

    @Transactional
    public ComprobanteResponseDTO emitirComprobante(PagoRequestDTO request) {
        Trabajador cajero = obtenerTrabajadorAutenticado();
        validarRol(cajero, "CAJERO");
        validarActivo(cajero, "El cajero autenticado no esta activo.");

        AperturaCaja apertura = aperturaCajaRepository
                .findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(cajero.getId(), AperturaCaja.EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalStateException(
                        "No puede procesar pagos porque no tiene una caja abierta."));

        List<OrdenServicio> deudas = obtenerDeudasSeleccionadas(request.getDeudaIds());
        validarDeudasPendientes(deudas);
        Paciente paciente = validarMismoPaciente(deudas);

        AsignacionCaja asignacion = obtenerAsignacionActiva(cajero.getId());
        SerieComprobante serie = obtenerSerieAsignada(asignacion, request.getTipoComprobante());
        int correlativo = serie.getCorrelativoActual() + 1;
        serie.setCorrelativoActual(correlativo);
        serieComprobanteRepository.save(serie);

        BigDecimal total = deudas.stream()
                .map(OrdenServicio::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Comprobante comprobante = Comprobante.builder()
                .serieComprobante(serie)
                .correlativo(correlativo)
                .numeroCompleto(serie.getSerie() + "-" + String.format("%0" + LONGITUD_CORRELATIVO + "d", correlativo))
                .ordenServicio(deudas.get(0))
                .aperturaCaja(apertura)
                .paciente(paciente)
                .rucDni(valorO(request.getRucDni(), paciente.getDni()))
                .razonSocialNombre(valorO(request.getRazonSocialNombre(), paciente.getNombreCompleto()))
                .direccionFiscal(valorLimpio(request.getDireccionFiscal()))
                .subtotal(total)
                .igv(BigDecimal.ZERO)
                .total(total)
                .estado(Comprobante.EstadoComprobante.EMITIDO)
                .emitidoPor(cajero)
                .build();
        comprobante = comprobanteRepository.save(comprobante);

        for (OrdenServicio deuda : deudas) {
            detalleComprobanteRepository.save(DetalleComprobante.builder()
                    .comprobante(comprobante)
                    .descripcion(deudaService.labelConcepto(deudaService.inferirConcepto(deuda))
                            + " - " + deuda.getNumeroOrden())
                    .cantidad(1)
                    .precioUnitario(deuda.getTotal())
                    .descuento(BigDecimal.ZERO)
                    .subtotal(deuda.getTotal())
                    .build());

            pagoRepository.save(Pago.builder()
                    .ordenServicio(deuda)
                    .aperturaCaja(apertura)
                    .comprobante(comprobante)
                    .metodoPago(request.getMetodoPago())
                    .monto(deuda.getTotal())
                    .referencia(valorLimpio(request.getReferencia()))
                    .observaciones("Pago generado desde caja")
                    .registradoPor(cajero)
                    .fechaPago(LocalDateTime.now())
                    .build());

            deuda.setEstado(OrdenServicio.EstadoOrden.PAGADA);
            if (deuda.getCita() != null && deuda.getCita().getEstado() == Cita.EstadoCita.PROGRAMADA) {
                deuda.getCita().setEstado(Cita.EstadoCita.CONFIRMADA);
            }
        }
        ordenServicioRepository.saveAll(deudas);

        return mapComprobante(comprobante);
    }

    @Transactional
    public AperturaCajaResponseDTO cuadrarCaja(CuadreCajaRequestDTO request) {
        Trabajador cajero = obtenerTrabajadorAutenticado();
        validarRol(cajero, "CAJERO");

        AperturaCaja apertura = aperturaCajaRepository
                .findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(cajero.getId(), AperturaCaja.EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalStateException("No tiene una caja abierta para cuadrar."));

        TotalesCaja totales = calcularTotales(apertura);
        BigDecimal dineroContado = request.getDineroContado();
        if (dineroContado == null || dineroContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El dinero contado debe ser mayor o igual a 0.00.");
        }

        apertura.setMontoCierre(dineroContado);
        apertura.setDiferencia(dineroContado.subtract(totales.totalTeorico()));
        apertura.setEstado(AperturaCaja.EstadoCaja.CUADRADA);
        apertura.setFechaCierre(LocalDateTime.now());
        apertura.setObservaciones(valorLimpio(request.getObservaciones()));

        return mapApertura(aperturaCajaRepository.save(apertura));
    }

    @Transactional(readOnly = true)
    public List<AperturaCajaResponseDTO> listarCuadresPendientes() {
        return aperturaCajaRepository.findByEstadoOrderByFechaAperturaDesc(AperturaCaja.EstadoCaja.CUADRADA).stream()
                .map(this::mapApertura)
                .toList();
    }

    @Transactional
    public AperturaCajaResponseDTO cerrarCaja(Long aperturaId) {
        AperturaCaja apertura = aperturaCajaRepository.findById(aperturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apertura de caja no encontrada."));
        if (apertura.getEstado() != AperturaCaja.EstadoCaja.CUADRADA) {
            throw new IllegalStateException("Solo se pueden cerrar cajas previamente cuadradas.");
        }
        apertura.setEstado(AperturaCaja.EstadoCaja.CERRADA);
        apertura.setFechaCierre(apertura.getFechaCierre() == null ? LocalDateTime.now() : apertura.getFechaCierre());
        return mapApertura(aperturaCajaRepository.save(apertura));
    }

    @Transactional(readOnly = true)
    public List<ComprobanteResponseDTO> listarComprobantes() {
        return comprobanteRepository.findAllByOrderByFechaEmisionDesc().stream()
                .map(this::mapComprobante)
                .toList();
    }

    @Transactional
    public ComprobanteResponseDTO anularComprobante(Long comprobanteId, AnularComprobanteRequestDTO request) {
        Comprobante comprobante = comprobanteRepository.findById(comprobanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante no encontrado."));
        if (comprobante.getEstado() == Comprobante.EstadoComprobante.ANULADO) {
            throw new IllegalStateException("El comprobante ya esta anulado.");
        }

        comprobante.setEstado(Comprobante.EstadoComprobante.ANULADO);
        comprobante.setMotivoAnulacion(request.getMotivo().trim());

        pagoRepository.findByComprobanteId(comprobante.getId()).forEach(pago -> {
            OrdenServicio deuda = pago.getOrdenServicio();
            deuda.setEstado(OrdenServicio.EstadoOrden.PENDIENTE);
            if (deuda.getCita() != null && deuda.getCita().getEstado() == Cita.EstadoCita.CONFIRMADA) {
                deuda.getCita().setEstado(Cita.EstadoCita.PROGRAMADA);
            }
            ordenServicioRepository.save(deuda);
        });

        return mapComprobante(comprobanteRepository.save(comprobante));
    }

    private AsignacionCaja construirAsignacion(
            AsignacionCaja asignacion,
            AsignacionCajaRequestDTO request,
            Trabajador cajero) {
        Caja caja = obtenerOCrearCaja(request);
        SerieComprobante boleta = obtenerSerieActiva(request.getSerieBoletaId(), SerieComprobante.TipoComprobante.BOLETA);
        SerieComprobante factura = obtenerSerieActiva(request.getSerieFacturaId(), SerieComprobante.TipoComprobante.FACTURA);

        boleta.setCaja(caja);
        factura.setCaja(caja);
        serieComprobanteRepository.saveAll(List.of(boleta, factura));

        asignacion.setCajero(cajero);
        asignacion.setCaja(caja);
        asignacion.setSerieBoleta(boleta);
        asignacion.setSerieFactura(factura);
        asignacion.setActivo(request.getActivo() == null || request.getActivo());
        return asignacion;
    }

    private Caja obtenerOCrearCaja(AsignacionCajaRequestDTO request) {
        if (request.getCajaId() != null) {
            Caja caja = cajaRepository.findById(request.getCajaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Caja no encontrada."));
            if (!caja.isActivo()) {
                throw new IllegalStateException("La caja seleccionada esta inactiva.");
            }
            return caja;
        }
        if (request.getCajaNombre() == null || request.getCajaNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de caja es obligatorio.");
        }
        return cajaRepository.save(Caja.builder()
                .nombre(request.getCajaNombre().trim())
                .ubicacion(valorLimpio(request.getCajaUbicacion()))
                .activo(true)
                .build());
    }

    private SerieComprobante obtenerSerieActiva(Long id, SerieComprobante.TipoComprobante tipo) {
        SerieComprobante serie = serieComprobanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Serie de comprobante no encontrada."));
        if (!serie.isActivo()) {
            throw new IllegalStateException("La serie " + serie.getSerie() + " esta inactiva.");
        }
        if (serie.getTipoComprobante() != tipo) {
            throw new IllegalArgumentException("La serie " + serie.getSerie() + " no corresponde a " + tipo.name() + ".");
        }
        return serie;
    }

    private Trabajador obtenerCajeroActivo(Long cajeroId) {
        Trabajador cajero = trabajadorRepository.findById(cajeroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajero no encontrado."));
        validarRol(cajero, "CAJERO");
        validarActivo(cajero, "Un cajero inactivo no puede tener series asignadas.");
        return cajero;
    }

    private Trabajador obtenerTrabajadorAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getName())) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado.");
        }
        return trabajadorRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
    }

    private void validarRol(Trabajador trabajador, String rol) {
        String nombreRol = trabajador.getRol() == null ? "" : trabajador.getRol().getNombre();
        if (!rol.equalsIgnoreCase(nombreRol)) {
            throw new IllegalArgumentException("La operacion requiere rol " + rol + ".");
        }
    }

    private void validarActivo(Trabajador trabajador, String mensaje) {
        if (!trabajador.isActivo()) {
            throw new IllegalStateException(mensaje);
        }
    }

    private AsignacionCaja obtenerAsignacionActiva(Long cajeroId) {
        return asignacionCajaRepository.findFirstByCajeroIdAndActivoTrueOrderByIdDesc(cajeroId)
                .orElseThrow(() -> new IllegalStateException(
                        "El cajero no tiene una caja y series activas asignadas."));
    }

    private SerieComprobante obtenerSerieAsignada(
            AsignacionCaja asignacion,
            SerieComprobante.TipoComprobante tipo) {
        if (tipo == SerieComprobante.TipoComprobante.BOLETA) {
            validarSerieOperativa(asignacion.getSerieBoleta(), SerieComprobante.TipoComprobante.BOLETA);
            return asignacion.getSerieBoleta();
        }
        if (tipo == SerieComprobante.TipoComprobante.FACTURA) {
            validarSerieOperativa(asignacion.getSerieFactura(), SerieComprobante.TipoComprobante.FACTURA);
            return asignacion.getSerieFactura();
        }
        throw new IllegalArgumentException("Solo se pueden emitir BOLETA o FACTURA.");
    }

    private void validarSerieOperativa(SerieComprobante serie, SerieComprobante.TipoComprobante tipo) {
        if (serie == null || !serie.isActivo() || serie.getTipoComprobante() != tipo) {
            throw new IllegalStateException("La serie asignada para " + tipo.name() + " no esta operativa.");
        }
    }

    private List<OrdenServicio> obtenerDeudasSeleccionadas(List<Long> deudaIds) {
        if (deudaIds == null || deudaIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una deuda.");
        }
        Set<Long> idsUnicos = new LinkedHashSet<>(deudaIds);
        List<OrdenServicio> deudas = ordenServicioRepository.findByIdIn(idsUnicos);
        if (deudas.size() != idsUnicos.size()) {
            throw new RecursoNoEncontradoException("Una o mas deudas seleccionadas no existen.");
        }
        return deudas;
    }

    private void validarDeudasPendientes(List<OrdenServicio> deudas) {
        deudas.forEach(deuda -> {
            if (deuda.getEstado() != OrdenServicio.EstadoOrden.PENDIENTE) {
                throw new IllegalStateException("La deuda " + deuda.getNumeroOrden() + " ya no esta pendiente.");
            }
        });
    }

    private Paciente validarMismoPaciente(List<OrdenServicio> deudas) {
        Paciente paciente = deudas.get(0).getPaciente();
        boolean todosMismoPaciente = deudas.stream()
                .allMatch(deuda -> deuda.getPaciente().getId().equals(paciente.getId()));
        if (!todosMismoPaciente) {
            throw new IllegalArgumentException("Solo puede pagar deudas de un mismo paciente en un comprobante.");
        }
        return paciente;
    }

    private TotalesCaja calcularTotales(AperturaCaja apertura) {
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjetas = BigDecimal.ZERO;
        BigDecimal billeteras = BigDecimal.ZERO;

        for (Pago pago : pagoRepository.findByAperturaCajaId(apertura.getId())) {
            if (pago.getComprobante() != null
                    && pago.getComprobante().getEstado() == Comprobante.EstadoComprobante.ANULADO) {
                continue;
            }
            if (pago.getMetodoPago() == Pago.MetodoPago.EFECTIVO) {
                efectivo = efectivo.add(pago.getMonto());
            } else if (pago.getMetodoPago() == Pago.MetodoPago.TARJETA_CREDITO
                    || pago.getMetodoPago() == Pago.MetodoPago.TARJETA_DEBITO) {
                tarjetas = tarjetas.add(pago.getMonto());
            } else if (pago.getMetodoPago() == Pago.MetodoPago.TRANSFERENCIA) {
                billeteras = billeteras.add(pago.getMonto());
            }
        }

        BigDecimal totalIngresos = efectivo.add(tarjetas).add(billeteras);
        BigDecimal totalTeorico = apertura.getMontoApertura().add(efectivo);
        return new TotalesCaja(efectivo, tarjetas, billeteras, totalIngresos, totalTeorico);
    }

    private AperturaCajaResponseDTO mapApertura(AperturaCaja apertura) {
        TotalesCaja totales = calcularTotales(apertura);
        return AperturaCajaResponseDTO.builder()
                .id(apertura.getId())
                .cajaId(apertura.getCaja().getId())
                .cajaNombre(apertura.getCaja().getNombre())
                .cajeroId(apertura.getCajero().getId())
                .cajeroNombre(apertura.getCajero().getNombreCompleto())
                .cajeroUsername(apertura.getCajero().getUsername())
                .montoInicial(apertura.getMontoApertura())
                .montoCierre(apertura.getMontoCierre())
                .diferencia(apertura.getDiferencia())
                .estado(apertura.getEstado().name())
                .fechaApertura(apertura.getFechaApertura())
                .fechaCierre(apertura.getFechaCierre())
                .totalEfectivo(totales.efectivo())
                .totalTarjetas(totales.tarjetas())
                .totalBilleteras(totales.billeteras())
                .totalIngresos(totales.totalIngresos())
                .totalTeorico(totales.totalTeorico())
                .build();
    }

    private ComprobanteResponseDTO mapComprobante(Comprobante comprobante) {
        List<Pago> pagos = pagoRepository.findByComprobanteId(comprobante.getId());
        List<DeudaResponseDTO> deudas = pagos.stream()
                .map(Pago::getOrdenServicio)
                .distinct()
                .map(deudaService::map)
                .toList();

        String metodoPago = pagos.isEmpty() ? null : pagos.get(0).getMetodoPago().name();
        return ComprobanteResponseDTO.builder()
                .id(comprobante.getId())
                .numeroCompleto(comprobante.getNumeroCompleto())
                .tipoComprobante(comprobante.getSerieComprobante().getTipoComprobante().name())
                .estado(comprobante.getEstado().name())
                .subtotal(comprobante.getSubtotal())
                .igv(comprobante.getIgv())
                .total(comprobante.getTotal())
                .pacienteDni(comprobante.getPaciente().getDni())
                .pacienteNombre(comprobante.getPaciente().getNombreCompleto())
                .emitidoPor(comprobante.getEmitidoPor().getNombreCompleto())
                .metodoPago(metodoPago)
                .fechaEmision(comprobante.getFechaEmision())
                .deudas(deudas)
                .build();
    }

    private AsignacionCajaResponseDTO mapAsignacion(AsignacionCaja asignacion) {
        return AsignacionCajaResponseDTO.builder()
                .id(asignacion.getId())
                .cajeroId(asignacion.getCajero().getId())
                .cajeroNombre(asignacion.getCajero().getNombreCompleto())
                .cajeroUsername(asignacion.getCajero().getUsername())
                .cajaId(asignacion.getCaja().getId())
                .cajaNombre(asignacion.getCaja().getNombre())
                .cajaUbicacion(asignacion.getCaja().getUbicacion())
                .serieBoletaId(asignacion.getSerieBoleta().getId())
                .serieBoleta(asignacion.getSerieBoleta().getSerie())
                .serieFacturaId(asignacion.getSerieFactura().getId())
                .serieFactura(asignacion.getSerieFactura().getSerie())
                .activo(asignacion.isActivo())
                .fechaAsignacion(asignacion.getFechaAsignacion())
                .build();
    }

    private CajaResponseDTO mapCaja(Caja caja) {
        return CajaResponseDTO.builder()
                .id(caja.getId())
                .nombre(caja.getNombre())
                .ubicacion(caja.getUbicacion())
                .activo(caja.isActivo())
                .build();
    }

    private TrabajadorResponseDTO mapTrabajador(Trabajador trabajador) {
        return TrabajadorResponseDTO.builder()
                .id(trabajador.getId())
                .dni(trabajador.getDni())
                .nombreCompleto(trabajador.getNombreCompleto())
                .username(trabajador.getUsername())
                .email(trabajador.getEmail())
                .telefono(trabajador.getTelefono())
                .fechaNacimiento(trabajador.getFechaNacimiento())
                .colegiatura(trabajador.getColegiatura())
                .rolId(trabajador.getRol().getId())
                .nombreRol(trabajador.getRol().getNombre())
                .activo(trabajador.isActivo())
                .especialidades(List.of())
                .build();
    }

    private String valorLimpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String valorO(String valor, String fallback) {
        String limpio = valorLimpio(valor);
        return limpio == null ? fallback : limpio;
    }

    private record TotalesCaja(
            BigDecimal efectivo,
            BigDecimal tarjetas,
            BigDecimal billeteras,
            BigDecimal totalIngresos,
            BigDecimal totalTeorico) {
    }
}
