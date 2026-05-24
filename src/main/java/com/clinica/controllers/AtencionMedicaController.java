package com.clinica.controllers;

import com.clinica.dtos.AtencionMedicaHistorialDTO;
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

    /**
     * Recibe los datos desde Angular y registra los resultados de la atención médica.
     */
    @PostMapping("/registro")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<Long> registrarAtencion(@RequestBody com.clinica.dtos.AtencionMedicaRequestDTO request) {
        Long idGenerado = atencionService.registrarAtencion(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(idGenerado);
    }


    /**
     * Endpoint para validar si el código ingresado existe como Cita u Orden de Emergencia.
     */
    @GetMapping("/verificar-cita/{codigo}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> verificarCitaUOrden(@PathVariable String codigo) {
        // Llama al nuevo método que devuelve un String
        String estado = atencionService.verificarEstadoCitaUOrden(codigo);
        
        // Lo empaqueta en un JSON: {"estado": "VALIDA"}
        return ResponseEntity.ok(Collections.singletonMap("estado", estado));
    }
}