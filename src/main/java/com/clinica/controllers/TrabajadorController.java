package com.clinica.controllers;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.services.TrabajadorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<TrabajadorResponseDTO> registrar(@Valid @RequestBody TrabajadorRequestDTO dto) {
        return ResponseEntity.ok(trabajadorService.registrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<TrabajadorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(trabajadorService.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Byte id) {
        trabajadorService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }
}