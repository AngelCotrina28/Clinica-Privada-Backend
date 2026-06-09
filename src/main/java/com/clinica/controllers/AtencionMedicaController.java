package com.clinica.controllers;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.dtos.CitaOpcionDTO;
import com.clinica.services.AtencionMedicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;
@RestController
@RequestMapping("/api/atenciones")
@RequiredArgsConstructor
public class AtencionMedicaController {

    private final AtencionMedicaService atencionService;

    @GetMapping("/historial/{historiaId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<AtencionMedicaHistorialDTO>> obtenerHistorial(@PathVariable Long historiaId) {
        List<AtencionMedicaHistorialDTO> historial = atencionService.obtenerHistorialPorPaciente(historiaId);
        return ResponseEntity.ok(historial);
    }

    @PostMapping("/registro")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<Long> registrarAtencion(@RequestBody com.clinica.dtos.AtencionMedicaRequestDTO request) {
        Long idGenerado = atencionService.registrarAtencion(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(idGenerado);
    }

    @GetMapping("/verificar-cita/{codigo}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> verificarCitaUOrden(@PathVariable String codigo) {
        String estado = atencionService.verificarEstadoCitaUOrden(codigo);
        
        return ResponseEntity.ok(Collections.singletonMap("estado", estado));
    }
    
    @GetMapping("/citas-disponibles/{historiaId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<CitaOpcionDTO>> getCitasDisponibles(
            @PathVariable Long historiaId) {
        return ResponseEntity.ok(atencionService.obtenerCitasDisponibles(historiaId));
    }

}