package com.clinica.controllers;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.services.TrabajadorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@RequiredArgsConstructor
public class TrabajadorController {

    private final TrabajadorService trabajadorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TrabajadorResponseDTO> crear(@Valid @RequestBody TrabajadorRequestDTO request) {
        return new ResponseEntity<>(trabajadorService.crear(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<TrabajadorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(trabajadorService.listarTodos());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TrabajadorResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TrabajadorRequestDTO request) {
        return ResponseEntity.ok(trabajadorService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id) {
        trabajadorService.cambiarEstado(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medicos/activos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TrabajadorResponseDTO>> listarMedicosActivos() {
        return ResponseEntity.ok(trabajadorService.listarMedicosActivos());
    }
}
