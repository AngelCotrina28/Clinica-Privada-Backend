package com.clinica.controllers;

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
import com.clinica.services.CajaFacturacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaFacturacionController {

    private final CajaFacturacionService cajaService;

    @GetMapping("/deudas")
    @PreAuthorize("hasAnyRole('CAJERO','ADMINISTRADOR')")
    public ResponseEntity<List<DeudaResponseDTO>> listarDeudasPendientes(
            @RequestParam String dni,
            @RequestParam(required = false) String concepto) {
        return ResponseEntity.ok(cajaService.listarDeudasPendientes(dni, concepto));
    }

    @GetMapping("/apertura-actual")
    @PreAuthorize("hasRole('CAJERO')")
    public ResponseEntity<AperturaCajaResponseDTO> obtenerAperturaActual() {
        return ResponseEntity.ok(cajaService.obtenerAperturaActual());
    }

    @PostMapping("/aperturas")
    @PreAuthorize("hasRole('CAJERO')")
    public ResponseEntity<AperturaCajaResponseDTO> abrirCaja(
            @Valid @RequestBody AperturaCajaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrirCaja(request));
    }

    @PostMapping("/pagos")
    @PreAuthorize("hasRole('CAJERO')")
    public ResponseEntity<ComprobanteResponseDTO> emitirComprobante(
            @Valid @RequestBody PagoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.emitirComprobante(request));
    }

    @PostMapping("/cuadre")
    @PreAuthorize("hasRole('CAJERO')")
    public ResponseEntity<AperturaCajaResponseDTO> cuadrarCaja(
            @Valid @RequestBody CuadreCajaRequestDTO request) {
        return ResponseEntity.ok(cajaService.cuadrarCaja(request));
    }

    @GetMapping("/cajas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<CajaResponseDTO>> listarCajasActivas() {
        return ResponseEntity.ok(cajaService.listarCajasActivas());
    }

    @GetMapping("/cajeros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<TrabajadorResponseDTO>> listarCajerosActivos() {
        return ResponseEntity.ok(cajaService.listarCajerosActivos());
    }

    @GetMapping("/asignaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AsignacionCajaResponseDTO>> listarAsignaciones() {
        return ResponseEntity.ok(cajaService.listarAsignaciones());
    }

    @PostMapping("/asignaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionCajaResponseDTO> crearAsignacion(
            @Valid @RequestBody AsignacionCajaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.crearAsignacion(request));
    }

    @PutMapping("/asignaciones/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionCajaResponseDTO> actualizarAsignacion(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionCajaRequestDTO request) {
        return ResponseEntity.ok(cajaService.actualizarAsignacion(id, request));
    }

    @PatchMapping("/asignaciones/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignacionCajaResponseDTO> cambiarEstadoAsignacion(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(cajaService.cambiarEstadoAsignacion(id, activo));
    }

    @GetMapping("/cuadres-pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AperturaCajaResponseDTO>> listarCuadresPendientes() {
        return ResponseEntity.ok(cajaService.listarCuadresPendientes());
    }

    @PostMapping("/cierres/{aperturaId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AperturaCajaResponseDTO> cerrarCaja(@PathVariable Long aperturaId) {
        return ResponseEntity.ok(cajaService.cerrarCaja(aperturaId));
    }

    @GetMapping("/comprobantes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ComprobanteResponseDTO>> listarComprobantes() {
        return ResponseEntity.ok(cajaService.listarComprobantes());
    }

    @PostMapping("/comprobantes/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ComprobanteResponseDTO> anularComprobante(
            @PathVariable Long id,
            @Valid @RequestBody AnularComprobanteRequestDTO request) {
        return ResponseEntity.ok(cajaService.anularComprobante(id, request));
    }
}
