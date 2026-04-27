package com.clinica.services;

import com.clinica.dtos.MedicamentoRequestDTO;
import com.clinica.dtos.MedicamentoResponseDTO;
import com.clinica.dtos.PageResponseDTO;

import com.clinica.model.entities.CategoriaMedicamento;
import com.clinica.model.entities.HistorialMedicamento;
import com.clinica.model.entities.Medicamento;
import com.clinica.model.entities.Trabajador;


import com.clinica.mappers.MedicamentoMapper;
import com.clinica.model.repositories.CategoriaMedicamentoRepository;
import com.clinica.model.repositories.HistorialMedicamentoRepository;
import com.clinica.model.repositories.MedicamentoRepository;
import com.clinica.model.repositories.TrabajadorRepository;

import com.clinica.exceptions.CodigoDuplicadoException;
import com.clinica.exceptions.MedicamentoInactivoException;
import com.clinica.exceptions.RecursoNoEncontradoException;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private final MedicamentoRepository       medicamentoRepo;
    private final CategoriaMedicamentoRepository categoriaRepo;
    private final HistorialMedicamentoRepository historialRepo;
    private final TrabajadorRepository           TrabajadorRepo;
    private final MedicamentoMapper           mapper;

    // ─── LISTAR / BUSCAR ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponseDTO<MedicamentoResponseDTO> buscar(
            String nombre, String codigo, Integer categoriaId,
            boolean soloActivos, int pagina, int tamano, String ordenarPor) {

        Sort sort = Sort.by(Sort.Direction.ASC, ordenarPor != null ? ordenarPor : "nombre");
        Pageable pageable = PageRequest.of(pagina, tamano, sort);

        Page<MedicamentoResponseDTO> resultado = medicamentoRepo
                .buscar(nombre, codigo, categoriaId, soloActivos, pageable)
                .map(mapper::toResponse);

        return PageResponseDTO.of(resultado);
    }

    @Transactional(readOnly = true)
    public MedicamentoResponseDTO obtenerPorId(Long id) {
        return mapper.toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MedicamentoResponseDTO> stockBajo(int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("stockActual"));
        return PageResponseDTO.of(medicamentoRepo.findStockBajo(pageable).map(mapper::toResponse));
    }

    // ─── REGISTRAR ───────────────────────────────────────────────────────────

    @Transactional
    public MedicamentoResponseDTO registrar(MedicamentoRequestDTO dto) {
        // Regla: código único
        if (medicamentoRepo.existsByCodigo(dto.getCodigo())) {
            throw new CodigoDuplicadoException("Ya existe un medicamento con el código: " + dto.getCodigo());
        }

        CategoriaMedicamento categoria = obtenerCategoria(dto.getCategoriaId());
        Trabajador TrabajadorActual = getTrabajadorAutenticado();

        Medicamento medicamento = Medicamento.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .nombreGenerico(dto.getNombreGenerico())
                .descripcion(dto.getDescripcion())
                .categoria(categoria)
                .presentacion(dto.getPresentacion())
                .laboratorio(dto.getLaboratorio())
                .precioUnitario(dto.getPrecioUnitario())   // @DecimalMin(0.01) ya validado en DTO
                .stockActual(dto.getStockInicial())         // @Min(0) ya validado en DTO
                .stockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0)
                .requiereReceta(dto.isRequiereReceta())
                .activo(true)
                .createdBy(TrabajadorActual)
                .build();

        medicamento = medicamentoRepo.save(medicamento);

        registrarHistorial(medicamento, HistorialMedicamento.TipoOperacion.CREACION,
                null, null, null);

        log.info("Medicamento registrado: {} por Trabajador {}", medicamento.getCodigo(), TrabajadorActual.getUsername());
        return mapper.toResponse(medicamento);
    }

    // ─── EDITAR ──────────────────────────────────────────────────────────────

    @Transactional
    public MedicamentoResponseDTO editar(Long id, MedicamentoRequestDTO dto) {
        Medicamento medicamento = obtenerEntidad(id);

        if (!medicamento.isActivo()) {
            throw new MedicamentoInactivoException("No se puede editar un medicamento inactivo.");
        }

        // Regla: código único (excluye el propio registro)
        if (medicamentoRepo.existsByCodigoAndIdNot(dto.getCodigo(), id)) {
            throw new CodigoDuplicadoException(
                    "Ya existe otro medicamento con el código: " + dto.getCodigo());
        }

        Trabajador TrabajadorActual = getTrabajadorAutenticado();

        // Auditoría campo a campo
        auditarCambio(medicamento, "precio_unitario",
                medicamento.getPrecioUnitario().toPlainString(),
                dto.getPrecioUnitario().toPlainString());
        auditarCambio(medicamento, "stock_actual",
                String.valueOf(medicamento.getStockActual()),
                String.valueOf(dto.getStockInicial()));

        // Aplicar cambios
        medicamento.setCodigo(dto.getCodigo());
        medicamento.setNombre(dto.getNombre());
        medicamento.setNombreGenerico(dto.getNombreGenerico());
        medicamento.setDescripcion(dto.getDescripcion());
        medicamento.setCategoria(obtenerCategoria(dto.getCategoriaId()));
        medicamento.setPresentacion(dto.getPresentacion());
        medicamento.setLaboratorio(dto.getLaboratorio());
        medicamento.setPrecioUnitario(dto.getPrecioUnitario());
        medicamento.setStockActual(dto.getStockInicial());
        medicamento.setStockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0);
        medicamento.setRequiereReceta(dto.isRequiereReceta());
        medicamento.setUpdatedBy(TrabajadorActual);

        medicamento = medicamentoRepo.save(medicamento);
        log.info("Medicamento editado: {} por {}", medicamento.getCodigo(), TrabajadorActual.getUsername());
        return mapper.toResponse(medicamento);
    }

    // ─── INACTIVAR ────────────────────────────────────────────────────────────

    @Transactional
    public MedicamentoResponseDTO inactivar(Long id) {
        Medicamento medicamento = obtenerEntidad(id);

        if (!medicamento.isActivo()) {
            throw new MedicamentoInactivoException("El medicamento ya se encuentra inactivo.");
        }

        medicamento.setActivo(false);
        medicamento.setUpdatedBy(getTrabajadorAutenticado());
        medicamento = medicamentoRepo.save(medicamento);

        registrarHistorial(medicamento, HistorialMedicamento.TipoOperacion.INACTIVACION,
                "activo", "true", "false");

        log.info("Medicamento inactivado: {}", medicamento.getCodigo());
        return mapper.toResponse(medicamento);
    }

    // ─── REACTIVAR ────────────────────────────────────────────────────────────

    @Transactional
    public MedicamentoResponseDTO activar(Long id) {
        Medicamento medicamento = obtenerEntidad(id);

        if (medicamento.isActivo()) {
            throw new IllegalStateException("El medicamento ya se encuentra activo.");
        }

        medicamento.setActivo(true);
        medicamento.setUpdatedBy(getTrabajadorAutenticado());
        medicamento = medicamentoRepo.save(medicamento);

        registrarHistorial(medicamento, HistorialMedicamento.TipoOperacion.ACTIVACION,
                "activo", "false", "true");
        return mapper.toResponse(medicamento);
    }

    // ─── CATEGORÍAS (solo lectura para el frontend) ───────────────────────────

    @Transactional(readOnly = true)
    public List<CategoriaMedicamento> listarCategorias() {
        return categoriaRepo.findAll(Sort.by("nombre"));
    }

    // ─── HISTORIAL ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<HistorialMedicamento> historial(Long medicamentoId, int pagina, int tamano) {
        return historialRepo.findByMedicamentoIdOrderByFechaOperacionDesc(
                medicamentoId, PageRequest.of(pagina, tamano));
    }

    // ─── HELPERS PRIVADOS ─────────────────────────────────────────────────────

    private Medicamento obtenerEntidad(Long id) {
        return medicamentoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medicamento no encontrado: " + id));
    }

    private CategoriaMedicamento obtenerCategoria(Integer catId) {
        return categoriaRepo.findById(catId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + catId));
    }

    private Trabajador getTrabajadorAutenticado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return TrabajadorRepo.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + username));
    }

    private void registrarHistorial(Medicamento med,
                                    HistorialMedicamento.TipoOperacion tipo,
                                    String campo, String anterior, String nuevo) {
        historialRepo.save(HistorialMedicamento.builder()
                .medicamento(med)
                .tipoOperacion(tipo)
                .campoModificado(campo)
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .Trabajador(getTrabajadorAutenticado())
                .build());
    }

    /** Solo registra auditoría si el valor realmente cambió */
    private void auditarCambio(Medicamento med, String campo, String anterior, String nuevo) {
        if (!anterior.equals(nuevo)) {
            registrarHistorial(med, HistorialMedicamento.TipoOperacion.EDICION, campo, anterior, nuevo);
        }
    }
}
