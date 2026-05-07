package com.clinica.controllers;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
import com.clinica.services.AtencionMedicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atenciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AtencionMedicaController {

    private final AtencionMedicaService atencionService;

    /**
     * Para obtener el historial clinico cronologico.
     * Acceso restringido a MEDICO y ADMINISTRADOR.
     */
    @GetMapping("/historial/{historiaId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<AtencionMedicaHistorialDTO>> obtenerHistorial(@PathVariable Long historiaId) {
        List<AtencionMedicaHistorialDTO> historial = atencionService.obtenerHistorialPorPaciente(historiaId);
        return ResponseEntity.ok(historial);
    }
}