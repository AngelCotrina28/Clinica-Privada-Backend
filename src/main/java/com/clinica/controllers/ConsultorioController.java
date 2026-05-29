package com.clinica.controllers;

import com.clinica.dtos.ConsultorioRequestDTO;
import com.clinica.dtos.ConsultorioResponseDTO;
import com.clinica.services.ConsultorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultorios")
@RequiredArgsConstructor
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ConsultorioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(consultorioService.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConsultorioResponseDTO> crear(@Valid @RequestBody ConsultorioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultorioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConsultorioResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConsultorioRequestDTO request) {
        return ResponseEntity.ok(consultorioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id) {
        consultorioService.cambiarEstado(id);
        return ResponseEntity.noContent().build();
    }
}
