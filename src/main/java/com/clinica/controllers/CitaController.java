package com.clinica.controllers;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.services.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<CitaResponseDTO> programarCita(@RequestBody CitaRequestDTO request) {
        return new ResponseEntity<>(citaService.programarCita(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CitaResponseDTO>> listar() {
        return ResponseEntity.ok(citaService.listarCitas());
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<com.clinica.dtos.DisponibilidadResponseDTO>> consultarDisponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        
        return ResponseEntity.ok(citaService.consultarDisponibilidad(medicoId, fechaInicio, fechaFin));
    }
}