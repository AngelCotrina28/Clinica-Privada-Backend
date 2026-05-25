package com.clinica.services;

import com.clinica.dtos.*;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.*;
import com.clinica.model.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmisionService {

        private final HistoriaClinicaRepository historiaRepo;
        private final OrdenAtencionEmergenciaRepository ordenRepo;
        private final TrabajadorRepository trabajadorRepo;

        @Transactional(readOnly = true)
        public HistoriaClinicaResponseDTO buscarPorDni(String dni) {
                return historiaRepo.findByDniPaciente(dni.trim())
                                .map(h -> toHistoriaDTO(h, false, false))
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "No se encontró historia clínica para el DNI: " + dni));
        }

        @Transactional(readOnly = true)
        public HistoriaClinicaResponseDTO buscarPorNumeroHistoria(String numeroHistoria) {
                return historiaRepo.findByNumeroHistoria(numeroHistoria.trim())
                                .map(h -> toHistoriaDTO(h, false, false))
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "No se encontró historia clínica con el número: " + numeroHistoria));
        }

        @Transactional
        public HistoriaClinicaResponseDTO abrirHistoria(AbrirHistoriaClinicaRequestDTO dto) {

                String dniNormalizado = dto.getDniPaciente().trim();

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

        @Transactional
        public OrdenAtencionEmergenciaResponseDTO generarOrdenEmergencia(GenerarOrdenEmergenciaRequestDTO dto) {

                HistoriaClinica historia = historiaRepo.findById(dto.getHistoriaClinicaId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Historia clínica no encontrada con ID: "
                                                                + dto.getHistoriaClinicaId()));

                Trabajador medico = trabajadorRepo.findById(dto.getMedicoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Médico no encontrado con ID: " + dto.getMedicoId()));

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

        @Transactional(readOnly = true)
        public PageResponseDTO<OrdenAtencionEmergenciaResponseDTO> auditarOrdenesEmergencia(
                        LocalDate desde,
                        LocalDate hasta,
                        String busqueda,
                        int pagina,
                        int tamano) {

                if (desde != null && hasta != null && desde.isAfter(hasta)) {
                        throw new IllegalArgumentException("La fecha Desde no puede ser mayor que la fecha Hasta.");
                }

                int paginaNormalizada = Math.max(pagina, 0);
                int tamanoNormalizado = tamano <= 0 ? 10 : Math.min(tamano, 50);
                LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
                LocalDateTime fin = hasta != null ? hasta.atTime(LocalTime.MAX) : null;
                String termino = busqueda == null || busqueda.isBlank()
                                ? null
                                : busqueda.trim().toLowerCase();

                Pageable pageable = PageRequest.of(paginaNormalizada, tamanoNormalizado);

                return PageResponseDTO.of(
                                ordenRepo.auditarOrdenes(inicio, fin, termino, pageable)
                                                .map(this::toOrdenDTO));
        }

        private Trabajador getTrabajadorAutenticado() {
                String username = SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                return trabajadorRepo.findByUsername(username)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Trabajador autenticado no encontrado: " + username));
        }

        private String generarNumeroHistoria() {
                long total = historiaRepo.count();
                return String.format("HC-%05d", total + 1);
        }

        private String generarNumeroOrden() {
                long total = ordenRepo.count();
                return String.format("OE-%05d", total + 1);
        }

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
