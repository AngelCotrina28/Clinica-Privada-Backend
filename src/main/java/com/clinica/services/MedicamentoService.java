package com.clinica.services;

import com.clinica.dtos.MedicamentoRequestDTO;
import com.clinica.dtos.MedicamentoResponseDTO;
import com.clinica.dtos.PageResponseDTO;
import com.clinica.exceptions.CodigoDuplicadoException;
import com.clinica.exceptions.MedicamentoInactivoException;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.mappers.MedicamentoMapper;
import com.clinica.model.entities.CategoriaMedicamento;
import com.clinica.model.entities.HistorialMedicamento;
import com.clinica.model.entities.Medicamento;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.CategoriaMedicamentoRepository;
import com.clinica.model.repositories.HistorialMedicamentoRepository;
import com.clinica.model.repositories.MedicamentoRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private static final int PAGE_SIZE_DEFAULT = 20;
    private static final int PAGE_SIZE_MAX = 100;
    private static final Map<String, String> CAMPOS_ORDEN_MEDICAMENTO = Map.of(
            "codigo", "codigo",
            "nombre", "nombre",
            "precioUnitario", "precioUnitario",
            "stockActual", "stockActual",
            "stockMinimo", "stockMinimo",
            "createdAt", "createdAt");

    private final MedicamentoRepository medicamentoRepo;
    private final CategoriaMedicamentoRepository categoriaRepo;
    private final HistorialMedicamentoRepository historialRepo;
    private final TrabajadorRepository trabajadorRepo;
    private final MedicamentoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<MedicamentoResponseDTO> buscar(
            String nombre, String codigo, Integer categoriaId,
            boolean soloActivos, int pagina, int tamano, String ordenarPor) {

        Pageable pageable = PageRequest.of(
                normalizarPagina(pagina),
                normalizarTamano(tamano),
                Sort.by(Sort.Direction.ASC, resolverCampoOrden(ordenarPor)));

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
        Pageable pageable = PageRequest.of(
                normalizarPagina(pagina),
                normalizarTamano(tamano),
                Sort.by("stockActual"));
        return PageResponseDTO.of(medicamentoRepo.findStockBajo(pageable).map(mapper::toResponse));
    }

    @Transactional
    public MedicamentoResponseDTO registrar(MedicamentoRequestDTO dto) {
        if (medicamentoRepo.existsByCodigo(dto.getCodigo())) {
            throw new CodigoDuplicadoException("Ya existe un medicamento con el codigo: " + dto.getCodigo());
        }

        CategoriaMedicamento categoria = obtenerCategoria(dto.getCategoriaId());
        Trabajador trabajadorActual = getTrabajadorAutenticado();

        Medicamento medicamento = Medicamento.builder()
                .codigo(dto.getCodigo())
                .nombre(dto.getNombre())
                .nombreGenerico(dto.getNombreGenerico())
                .descripcion(dto.getDescripcion())
                .categoria(categoria)
                .presentacion(dto.getPresentacion())
                .laboratorio(dto.getLaboratorio())
                .precioUnitario(dto.getPrecioUnitario())
                .stockActual(dto.getStockInicial())
                .stockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0)
                .requiereReceta(dto.isRequiereReceta())
                .activo(true)
                .createdBy(trabajadorActual)
                .build();

        medicamento = medicamentoRepo.save(medicamento);

        registrarHistorial(medicamento, HistorialMedicamento.TipoOperacion.CREACION,
                null, null, null);

        log.info("Medicamento registrado: {} por trabajador {}", medicamento.getCodigo(), trabajadorActual.getUsername());
        return mapper.toResponse(medicamento);
    }

    @Transactional
    public MedicamentoResponseDTO editar(Long id, MedicamentoRequestDTO dto) {
        Medicamento medicamento = obtenerEntidad(id);

        if (!medicamento.isActivo()) {
            throw new MedicamentoInactivoException("No se puede editar un medicamento inactivo.");
        }

        if (medicamentoRepo.existsByCodigoAndIdNot(dto.getCodigo(), id)) {
            throw new CodigoDuplicadoException("Ya existe otro medicamento con el codigo: " + dto.getCodigo());
        }

        Trabajador trabajadorActual = getTrabajadorAutenticado();

        auditarCambio(medicamento, "precio_unitario",
                medicamento.getPrecioUnitario().toPlainString(),
                dto.getPrecioUnitario().toPlainString());
        auditarCambio(medicamento, "stock_actual",
                String.valueOf(medicamento.getStockActual()),
                String.valueOf(dto.getStockInicial()));

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
        medicamento.setUpdatedBy(trabajadorActual);

        medicamento = medicamentoRepo.save(medicamento);
        log.info("Medicamento editado: {} por {}", medicamento.getCodigo(), trabajadorActual.getUsername());
        return mapper.toResponse(medicamento);
    }

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

    @Transactional(readOnly = true)
    public List<CategoriaMedicamento> listarCategorias() {
        return categoriaRepo.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Page<HistorialMedicamento> historial(Long medicamentoId, int pagina, int tamano) {
        obtenerEntidad(medicamentoId);
        return historialRepo.findByMedicamentoIdOrderByFechaOperacionDesc(
                medicamentoId, PageRequest.of(normalizarPagina(pagina), normalizarTamano(tamano)));
    }

    private Medicamento obtenerEntidad(Long id) {
        return medicamentoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medicamento no encontrado: " + id));
    }

    private CategoriaMedicamento obtenerCategoria(Integer catId) {
        return categoriaRepo.findById(catId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada: " + catId));
    }

    private Trabajador getTrabajadorAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("No se pudo identificar al trabajador autenticado.");
        }

        String username = authentication.getName();
        return trabajadorRepo.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Trabajador autenticado no encontrado: " + username));
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

    private void auditarCambio(Medicamento med, String campo, String anterior, String nuevo) {
        if (!anterior.equals(nuevo)) {
            registrarHistorial(med, HistorialMedicamento.TipoOperacion.EDICION, campo, anterior, nuevo);
        }
    }

    private int normalizarPagina(int pagina) {
        return Math.max(pagina, 0);
    }

    private int normalizarTamano(int tamano) {
        if (tamano <= 0) {
            return PAGE_SIZE_DEFAULT;
        }
        return Math.min(tamano, PAGE_SIZE_MAX);
    }

    private String resolverCampoOrden(String ordenarPor) {
        if (ordenarPor == null || ordenarPor.isBlank()) {
            return "nombre";
        }
        String campo = CAMPOS_ORDEN_MEDICAMENTO.get(ordenarPor.trim());
        if (campo == null) {
            throw new IllegalArgumentException("Campo de ordenamiento no permitido: " + ordenarPor);
        }
        return campo;
    }
}
