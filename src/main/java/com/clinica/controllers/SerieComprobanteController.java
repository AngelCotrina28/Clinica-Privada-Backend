package com.clinica.controllers;

import com.clinica.dtos.SerieComprobanteRequestDTO;
import com.clinica.dtos.SerieComprobanteResponseDTO;
import com.clinica.services.SerieComprobanteService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/series-comprobantes")
@RequiredArgsConstructor
public class SerieComprobanteController {

    private final SerieComprobanteService serieService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SerieComprobanteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(serieService.listarTodos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SerieComprobanteResponseDTO> crear(
            @Valid @RequestBody SerieComprobanteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serieService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SerieComprobanteResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SerieComprobanteRequestDTO request) {
        return ResponseEntity.ok(serieService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id) {
        serieService.cambiarEstado(id);
        return ResponseEntity.noContent().build();
    }
}
