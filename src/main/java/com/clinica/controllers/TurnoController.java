package com.clinica.controllers;

import com.clinica.dtos.TurnoRequestDTO;
import com.clinica.dtos.TurnoResponseDTO;
import com.clinica.services.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<TurnoResponseDTO>> listar(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam int anio,
            @RequestParam int mes) {
        return ResponseEntity.ok(turnoService.listar(especialidadId, anio, mes));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TurnoResponseDTO> crear(@RequestBody TurnoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TurnoResponseDTO> actualizar(@PathVariable Long id, @RequestBody TurnoRequestDTO dto) {
        return ResponseEntity.ok(turnoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        turnoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
