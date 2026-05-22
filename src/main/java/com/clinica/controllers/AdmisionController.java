package com.clinica.controllers;

import com.clinica.dtos.*;
import com.clinica.services.AdmisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller REST — Módulo de Admisión y Consultas
 *
 * Endpoints:
 * GET /api/admision/historia?dni={dni} → buscar historia por DNI
 * POST /api/admision/historia → abrir nueva historia clínica
 * POST /api/admision/emergencia/orden → generar orden de atención de emergencia
 *
 * RBAC:
 * - Abrir historia: RECEPCIONISTA | ENFERMERO | ADMINISTRADOR | JEFE_ENFERMERIA
 * - Generar orden: JEFE_ENFERMERIA | ADMINISTRADOR
 */

@RestController
@RequestMapping("/api/admision")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdmisionController {

    private final AdmisionService admisionService;

    /**
     * Busca una historia clínica por DNI.
     * Todos los roles autenticados pueden consultar.
     */
    @GetMapping("/historia")
    public ResponseEntity<HistoriaClinicaResponseDTO> buscarHistoria(
            @RequestParam String dni) {
        return ResponseEntity.ok(admisionService.buscarPorDni(dni));
    }

    /**
     * Busca una historia clínica por Número de Historia (Ej: HC-00001).
     * Todos los roles autenticados pueden consultar.
     */
    @GetMapping("/historia/numero")
    public ResponseEntity<HistoriaClinicaResponseDTO> buscarHistoriaPorNumero(
            @RequestParam String numeroHistoria) {
        return ResponseEntity.ok(admisionService.buscarPorNumeroHistoria(numeroHistoria));
    }

    @GetMapping("/emergencia/ordenes/hoy")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ENFERMERO','ADMINISTRADOR','JEFE_ENFERMERIA')")
    public ResponseEntity<List<OrdenAtencionEmergenciaResponseDTO>> obtenerOrdenesHoy() {
        List<OrdenAtencionEmergenciaResponseDTO> ordenes = admisionService.obtenerOrdenesEmergenciaHoy();
        return ResponseEntity.ok(ordenes);
    }

    /**
     * Abre una nueva Historia Clínica.
     *
     * Roles permitidos: RECEPCIONISTA, ENFERMERO, ADMINISTRADOR, JEFE_ENFERMERIA
     *
     * Si el body incluye {@code desdeAdmision: true}, la respuesta retorna
     * un {@code redirectUrl} para que el frontend navegue automáticamente
     * al formulario de Generar Orden de Emergencia.
     */
    @PostMapping("/historia")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ENFERMERO','ADMINISTRADOR','JEFE_ENFERMERIA')")
    public ResponseEntity<HistoriaClinicaResponseDTO> abrirHistoria(
            @Valid @RequestBody AbrirHistoriaClinicaRequestDTO dto) {
        HistoriaClinicaResponseDTO response = admisionService.abrirHistoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Genera una Orden de Atención de Emergencia.
     *
     * Roles permitidos: JEFE_ENFERMERIA, ADMINISTRADOR
     */
    @PostMapping("/emergencia/orden")
    @PreAuthorize("hasAnyRole('JEFE_ENFERMERIA','ADMINISTRADOR')")
    public ResponseEntity<OrdenAtencionEmergenciaResponseDTO> generarOrdenEmergencia(
            @Valid @RequestBody GenerarOrdenEmergenciaRequestDTO dto) {
        OrdenAtencionEmergenciaResponseDTO response = admisionService.generarOrdenEmergencia(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}