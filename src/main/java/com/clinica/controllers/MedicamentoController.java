package com.clinica.controllers;

import com.clinica.dtos.*;
import com.clinica.services.MedicamentoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Catálogo de Medicamentos
 *
 * Endpoints:
 *  GET    /api/medicamentos              → listar/buscar (todos los roles)
 *  GET    /api/medicamentos/{id}         → detalle     (todos los roles)
 *  GET    /api/medicamentos/stock-bajo   → alertas     (todos los roles)
 *  POST   /api/medicamentos              → registrar   (solo ADMIN)
 *  PUT    /api/medicamentos/{id}         → editar      (solo ADMIN)
 *  PATCH  /api/medicamentos/{id}/inactivar → inactivar (solo ADMIN)
 *  PATCH  /api/medicamentos/{id}/activar   → activar   (solo ADMIN)
 *  GET    /api/medicamentos/{id}/historial → auditoría (ADMIN)
 *  GET    /api/categorias                → listado de categorías
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MedicamentoController {

    private final MedicamentoService service;

    // ── Consulta (cualquier usuario autenticado) ──────────────────────────────

    @GetMapping("/medicamentos")
    public ResponseEntity<PageResponseDTO<MedicamentoResponseDTO>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(defaultValue = "true")  boolean soloActivos,
            @RequestParam(defaultValue = "0")    int pagina,
            @RequestParam(defaultValue = "20")   int tamano,
            @RequestParam(defaultValue = "nombre") String ordenarPor) {

        return ResponseEntity.ok(
                service.buscar(nombre, codigo, categoriaId, soloActivos, pagina, tamano, ordenarPor));
    }

    @GetMapping("/medicamentos/{id}")
    public ResponseEntity<MedicamentoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/medicamentos/stock-bajo")
    public ResponseEntity<PageResponseDTO<MedicamentoResponseDTO>> stockBajo(
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(service.stockBajo(pagina, tamano));
    }

    @GetMapping("/categorias")
    public ResponseEntity<?> categorias() {
        return ResponseEntity.ok(service.listarCategorias());
    }

    // ── Gestión (solo ADMIN) ─────────────────────────────────────────────────

    @PostMapping("/medicamentos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> registrar(
            @Valid @RequestBody MedicamentoRequestDTO dto) {
        MedicamentoResponseDTO response = service.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/medicamentos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> editar(
            @PathVariable Long id,
            @Valid @RequestBody MedicamentoRequestDTO dto) {
        return ResponseEntity.ok(service.editar(id, dto));
    }

    @PatchMapping("/medicamentos/{id}/inactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> inactivar(@PathVariable Long id) {
        return ResponseEntity.ok(service.inactivar(id));
    }

    @PatchMapping("/medicamentos/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(service.activar(id));
    }

    @GetMapping("/medicamentos/{id}/historial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<?>> historial(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(service.historial(id, pagina, tamano));
    }
}
