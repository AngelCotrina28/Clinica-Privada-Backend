package com.clinica.services;

import com.clinica.dtos.ConsultorioRequestDTO;
import com.clinica.dtos.ConsultorioResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.Consultorio;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.repositories.ConsultorioRepository;
import com.clinica.model.repositories.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final EspecialidadRepository especialidadRepository;

    @Transactional(readOnly = true)
    public List<ConsultorioResponseDTO> listarTodos() {
        return consultorioRepository.findAll(Sort.by("id")).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public ConsultorioResponseDTO crear(ConsultorioRequestDTO request) {
        Consultorio consultorio = Consultorio.builder()
                .nombre(normalizarObligatorio(request.getNombre()))
                .numero(normalizarOpcional(request.getNumero()))
                .piso(normalizarOpcional(request.getPiso()))
                .especialidad(resolverEspecialidad(request.getEspecialidadId()))
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        return map(consultorioRepository.save(consultorio));
    }

    @Transactional
    public ConsultorioResponseDTO actualizar(Long id, ConsultorioRequestDTO request) {
        Consultorio consultorio = consultorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consultorio no encontrado."));

        consultorio.setNombre(normalizarObligatorio(request.getNombre()));
        consultorio.setNumero(normalizarOpcional(request.getNumero()));
        consultorio.setPiso(normalizarOpcional(request.getPiso()));
        consultorio.setEspecialidad(resolverEspecialidad(request.getEspecialidadId()));
        if (request.getActivo() != null) {
            consultorio.setActivo(request.getActivo());
        }

        return map(consultorioRepository.save(consultorio));
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Consultorio consultorio = consultorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consultorio no encontrado."));
        consultorio.setActivo(!consultorio.isActivo());
        consultorioRepository.save(consultorio);
    }

    private Especialidad resolverEspecialidad(Long especialidadId) {
        if (especialidadId == null) {
            return null;
        }
        return especialidadRepository.findById(especialidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada."));
    }

    private ConsultorioResponseDTO map(Consultorio consultorio) {
        Especialidad especialidad = consultorio.getEspecialidad();
        return ConsultorioResponseDTO.builder()
                .id(consultorio.getId())
                .nombre(consultorio.getNombre())
                .numero(consultorio.getNumero())
                .piso(consultorio.getPiso())
                .especialidadId(especialidad != null ? especialidad.getId() : null)
                .especialidad(especialidad != null ? especialidad.getNombre() : "General")
                .activo(consultorio.isActivo())
                .build();
    }

    private String normalizarObligatorio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
