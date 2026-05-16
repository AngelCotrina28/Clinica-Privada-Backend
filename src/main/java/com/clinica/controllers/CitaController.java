package com.clinica.controllers;

import com.clinica.dtos.CitaRequestDTO;
import com.clinica.dtos.CitaResponseDTO;
import com.clinica.dtos.HorarioBloqueDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.services.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    public ResponseEntity<List<CitaResponseDTO>> listar() {
        return ResponseEntity.ok(citaService.listarCitas());
    }

    @GetMapping("/medicos")
    public ResponseEntity<List<TrabajadorResponseDTO>> listarMedicosPorEspecialidad(
            @RequestParam Long especialidadId) {
        return ResponseEntity.ok(citaService.listarMedicosPorEspecialidad(especialidadId));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<HorarioBloqueDTO>> obtenerDisponibilidad(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerDisponibilidad(medicoId, fecha));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ENFERMERO','ADMINISTRADOR')")
    public ResponseEntity<CitaResponseDTO> programar(@Valid @RequestBody CitaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.programarCita(request));
    }
}
