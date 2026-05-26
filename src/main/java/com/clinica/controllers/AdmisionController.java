package com.clinica.controllers;

import com.clinica.dtos.*;
import com.clinica.services.AdmisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admision")
@RequiredArgsConstructor
public class AdmisionController {

    private final AdmisionService admisionService;

    @GetMapping("/historia")
    public ResponseEntity<HistoriaClinicaResponseDTO> buscarHistoria(
            @RequestParam String dni) {
        return ResponseEntity.ok(admisionService.buscarPorDni(dni));
    }

    @GetMapping("/historia/numero")
    public ResponseEntity<HistoriaClinicaResponseDTO> buscarHistoriaPorNumero(
            @RequestParam String numeroHistoria) {
        return ResponseEntity.ok(admisionService.buscarPorNumeroHistoria(numeroHistoria));
    }

    @GetMapping("/emergencia/ordenes")
    @PreAuthorize("hasAnyRole('JEFE_ENFERMERIA','ADMINISTRADOR')")
    public ResponseEntity<PageResponseDTO<OrdenAtencionEmergenciaResponseDTO>> auditarOrdenesEmergencia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                admisionService.auditarOrdenesEmergencia(desde, hasta, busqueda, page, size));
    }

    @GetMapping("/emergencia/ordenes/hoy")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ENFERMERO','ADMINISTRADOR','JEFE_ENFERMERIA')")
    public ResponseEntity<List<OrdenAtencionEmergenciaResponseDTO>> obtenerOrdenesHoy() {
        List<OrdenAtencionEmergenciaResponseDTO> ordenes = admisionService.obtenerOrdenesEmergenciaHoy();
        return ResponseEntity.ok(ordenes);
    }

    @PostMapping("/historia")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ENFERMERO','ADMINISTRADOR','JEFE_ENFERMERIA')")
    public ResponseEntity<HistoriaClinicaResponseDTO> abrirHistoria(
            @Valid @RequestBody AbrirHistoriaClinicaRequestDTO dto) {
        HistoriaClinicaResponseDTO response = admisionService.abrirHistoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/emergencia/orden")
    @PreAuthorize("hasAnyRole('JEFE_ENFERMERIA','ADMINISTRADOR')")
    public ResponseEntity<OrdenAtencionEmergenciaResponseDTO> generarOrdenEmergencia(
            @Valid @RequestBody GenerarOrdenEmergenciaRequestDTO dto) {
        OrdenAtencionEmergenciaResponseDTO response = admisionService.generarOrdenEmergencia(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
