package com.clinica.controllers;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.services.TrabajadorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Para permitir peticiones desde Angular
public class TrabajadorController {

    private final TrabajadorService trabajadorService;

    @PostMapping
    public ResponseEntity<TrabajadorResponseDTO> crear(@RequestBody TrabajadorRequestDTO request) {
        return new ResponseEntity<>(trabajadorService.crear(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TrabajadorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(trabajadorService.listarTodos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrabajadorResponseDTO> actualizar(@PathVariable Byte id, @RequestBody TrabajadorRequestDTO request) {
        return ResponseEntity.ok(trabajadorService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Byte id) {
        trabajadorService.cambiarEstado(id);
        return ResponseEntity.noContent().build();
    }
}