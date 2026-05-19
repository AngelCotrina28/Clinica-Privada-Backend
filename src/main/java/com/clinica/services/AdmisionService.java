package com.clinica.services;

import com.clinica.dtos.*;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.*;
import com.clinica.model.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Servicio de Admisión y Consultas.
 *
 * Responsabilidades (principio SRP):
 * 1. Buscar historia clínica por DNI.
 * 2. Abrir nueva historia clínica (con prevención de duplicados).
 * 3. Generar Orden de Atención de Emergencia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmisionService {

        private final HistoriaClinicaRepository historiaRepo;
        private final OrdenAtencionEmergenciaRepository ordenRepo;
        private final TrabajadorRepository trabajadorRepo;

        // 1. BUSCAR HISTORIA CLÍNICA

        @Transactional(readOnly = true)
        public HistoriaClinicaResponseDTO buscarPorDni(String dni) {
                return historiaRepo.findByDniPaciente(dni.trim())
                                .map(h -> toHistoriaDTO(h, false, false))
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "No se encontró historia clínica para el DNI: " + dni));
        }

        // 1.1 BUSCAR HISTORIA CLÍNICA POR NÚMERO (Optimización HU08)
        @Transactional(readOnly = true)
        public HistoriaClinicaResponseDTO buscarPorNumeroHistoria(String numeroHistoria) {
                return historiaRepo.findByNumeroHistoria(numeroHistoria.trim())
                                .map(h -> toHistoriaDTO(h, false, false))
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "No se encontró historia clínica con el número: " + numeroHistoria));
        }

        // 2. ABRIR NUEVA HISTORIA CLÍNICA

        /**
         * Abre una nueva historia clínica.
         *
         * <p>
         * Regla de negocio: el DNI es el identificador único del paciente.
         * Si ya existe una historia con ese DNI, lanza excepción (no se permite
         * duplicado).
         * </p>
         *
         * <p>
         * Flujo de Admisión: si {@code desdeAdmision = true}, el DTO de respuesta
         * incluye un {@code redirectUrl} para que el frontend redirija automáticamente
         * al formulario de Generar Orden de Emergencia con los datos del paciente
         * pre-cargados, evitando el reingreso manual.
         * </p>
         */
        @Transactional
        public HistoriaClinicaResponseDTO abrirHistoria(AbrirHistoriaClinicaRequestDTO dto) {

                String dniNormalizado = dto.getDniPaciente().trim();

                // ── Prevención de duplicados ─────────────────────────────────────────
                if (historiaRepo.existsByDniPaciente(dniNormalizado)) {
                        throw new IllegalStateException(
                                        "Ya existe una historia clínica para el DNI: " + dniNormalizado +
                                                        ". Use la opción 'Buscar Historia' para recuperarla.");
                }

                Trabajador autor = getTrabajadorAutenticado();
                String numeroHistoria = generarNumeroHistoria();

                HistoriaClinica historia = HistoriaClinica.builder()
                                .numeroHistoria(numeroHistoria)
                                .dniPaciente(dniNormalizado)
                                .nombreCompleto(dto.getNombreCompleto().trim())
                                .telefono(dto.getTelefono())
                                .email(dto.getEmail())
                                .fechaNacimiento(dto.getFechaNacimiento())
                                .genero(dto.getGenero())
                                .direccion(dto.getDireccion())
                                .creadoPor(autor)
                                .build();

                historia = historiaRepo.save(historia);
                log.info("Historia clínica creada: {} para paciente DNI: {} por usuario: {}",
                                numeroHistoria, dniNormalizado, autor.getUsername());

                return toHistoriaDTO(historia, true, dto.isDesdeAdmision());
        }

        // 3. GENERAR ORDEN DE ATENCIÓN DE EMERGENCIA ──────────────────────────

        /**
         * Genera una Orden de Atención de Emergencia.
         * Solo roles JEFE_ENFERMERIA y ADMINISTRADOR pueden invocar este método
         * (enforced en el controller con @PreAuthorize).
         */
        @Transactional
        public OrdenAtencionEmergenciaResponseDTO generarOrdenEmergencia(GenerarOrdenEmergenciaRequestDTO dto) {

                HistoriaClinica historia = historiaRepo.findById(dto.getHistoriaClinicaId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Historia clínica no encontrada con ID: "
                                                                + dto.getHistoriaClinicaId()));

                Trabajador medico = trabajadorRepo.findById(dto.getMedicoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Médico no encontrado con ID: " + dto.getMedicoId()));

                // Validar que sea efectivamente un médico activo
                if (!medico.getRol().getNombre().equalsIgnoreCase("MEDICO")) {
                        throw new IllegalArgumentException(
                                        "El trabajador seleccionado no tiene rol de Médico.");
                }
                if (!medico.isActivo()) {
                        throw new IllegalStateException(
                                        "El médico seleccionado no está activo.");
                }

                Trabajador autor = getTrabajadorAutenticado();
                String numeroOrden = generarNumeroOrden();

                OrdenAtencionEmergencia orden = OrdenAtencionEmergencia.builder()
                                .numeroOrden(numeroOrden)
                                .historiaClinica(historia)
                                .medico(medico)
                                .motivo(dto.getMotivo())
                                .generadoPor(autor)
                                .build();

                orden = ordenRepo.save(orden);
                log.info("Orden de emergencia generada: {} para historia: {} por usuario: {}",
                                numeroOrden, historia.getNumeroHistoria(), autor.getUsername());

                return toOrdenDTO(orden);
        }

        @Transactional(readOnly = true)
        public List<OrdenAtencionEmergenciaResponseDTO> obtenerOrdenesEmergenciaHoy() {
                LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
                LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);

                return ordenRepo.findByCreatedAtBetweenOrderByCreatedAtDesc(inicioDia, finDia)
                                .stream()
                                .map(this::toOrdenDTO)
                                .collect(Collectors.toList());
        }
        
        // ── HELPERS PRIVADOS ─────────────────────────────────────────────────────

        /** Obtiene el Trabajador autenticado desde el SecurityContext */
        private Trabajador getTrabajadorAutenticado() {
                String username = SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                return trabajadorRepo.findByUsername(username)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Trabajador autenticado no encontrado: " + username));
        }

        /**
         * Genera número de historia correlativo: HC-00001, HC-00002, ...
         * Para producción considerar una secuencia de BD o un campo autoincremental
         * dedicado.
         */
        private String generarNumeroHistoria() {
                long total = historiaRepo.count();
                return String.format("HC-%05d", total + 1);
        }

        /** Genera número de orden correlativo: OE-00001 */
        private String generarNumeroOrden() {
                long total = ordenRepo.count();
                return String.format("OE-%05d", total + 1);
        }

        /** Mapea HistoriaClinica → HistoriaClinicaResponseDTO */
        private HistoriaClinicaResponseDTO toHistoriaDTO(
                        HistoriaClinica h, boolean nuevaHistoria, boolean desdeAdmision) {

                HistoriaClinicaResponseDTO dto = HistoriaClinicaResponseDTO.builder()
                                .id(h.getId())
                                .numeroHistoria(h.getNumeroHistoria())
                                .dniPaciente(h.getDniPaciente())
                                .nombreCompleto(h.getNombreCompleto())
                                .telefono(h.getTelefono())
                                .email(h.getEmail())
                                .fechaNacimiento(h.getFechaNacimiento())
                                .genero(h.getGenero())
                                .direccion(h.getDireccion())
                                .creadoPor(h.getCreadoPor().getUsername())
                                .createdAt(h.getCreatedAt())
                                .nuevaHistoria(nuevaHistoria)
                                .build();

                // ── Flujo de Admisión: construir URL de redirección ─────────────────
                // El frontend usará esta URL para navegar automáticamente al formulario
                // de Generar Orden de Emergencia con los datos del paciente pre-cargados.
                if (desdeAdmision) {
                        String nombre = URLEncoder.encode(h.getNombreCompleto(), StandardCharsets.UTF_8);
                        String dni = URLEncoder.encode(h.getDniPaciente(), StandardCharsets.UTF_8);
                        dto.setRedirectUrl(
                                        "/admision/emergencia" +
                                                        "?historiaId=" + h.getId() +
                                                        "&numeroHistoria=" + encode(h.getNumeroHistoria()) +
                                                        "&nombre=" + nombre +
                                                        "&dni=" + dni);
                }

                return dto;
        }

        private String encode(String value) {
                return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        /** Mapea OrdenAtencionEmergencia → OrdenAtencionEmergenciaResponseDTO */
        private OrdenAtencionEmergenciaResponseDTO toOrdenDTO(OrdenAtencionEmergencia o) {
                HistoriaClinica h = o.getHistoriaClinica();
                Trabajador medico = o.getMedico();

                String especialidades = medico.getEspecialidades().stream()
                                .map(e -> e.getNombre())
                                .collect(Collectors.joining(", "));

                return OrdenAtencionEmergenciaResponseDTO.builder()
                                .id(o.getId())
                                .numeroOrden(o.getNumeroOrden())
                                .historiaClinicaId(h.getId())
                                .numeroHistoria(h.getNumeroHistoria())
                                .dniPaciente(h.getDniPaciente())
                                .nombrePaciente(h.getNombreCompleto())
                                .medicoId(medico.getId())
                                .nombreMedico(medico.getNombreCompleto())
                                .especialidadMedico(especialidades.isBlank() ? "Medicina General" : especialidades)
                                .estado(o.getEstado().name())
                                .motivo(o.getMotivo())
                                .generadoPor(o.getGeneradoPor().getUsername())
                                .createdAt(o.getCreatedAt())
                                .build();
        }
}