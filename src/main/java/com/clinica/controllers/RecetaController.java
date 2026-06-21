package com.clinica.controllers;

import com.clinica.dtos.RecetaRequestDTO;
import com.clinica.dtos.RecetaResponseDTO;
import com.clinica.services.RecetaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaService service;
    
    @GetMapping("/buscar")
    public ResponseEntity<List<RecetaResponseDTO>> buscar(@RequestParam String termino) {
        return ResponseEntity.ok(service.buscar(termino));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO','ADMINISTRADOR')")
    public ResponseEntity<RecetaResponseDTO> registrar(@Valid @RequestBody RecetaRequestDTO dto) {
        RecetaResponseDTO response = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/despachar")
    @PreAuthorize("hasAnyRole('FARMACIA','ADMINISTRADOR')")
    public ResponseEntity<RecetaResponseDTO> despachar(@PathVariable Long id) {
        return ResponseEntity.ok(service.despachar(id));
    }
}