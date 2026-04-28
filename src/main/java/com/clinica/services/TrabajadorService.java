package com.clinica.services;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.model.entities.Especialidad;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.EspecialidadRepository;
import com.clinica.model.repositories.RolRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private final EspecialidadRepository especialidadRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TrabajadorResponseDTO crear(TrabajadorRequestDTO dto) {
        // Validaciones de unicidad
        if (trabajadorRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }
        if (trabajadorRepository.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        
        if (rol.getNombre().equalsIgnoreCase("Médico")) {
            if (dto.getColegiatura() == null || dto.getColegiatura().isBlank()) {
                throw new RuntimeException("El número de colegiatura es obligatorio para el rol Médico.");
            }
        }

        Set<Especialidad> especialidades = new java.util.HashSet<>();
        if (rol.getNombre().equalsIgnoreCase("Médico") && dto.getEspecialidadesIds() != null && !dto.getEspecialidadesIds().isEmpty()) {
            especialidades = new java.util.HashSet<>(especialidadRepository.findAllById(dto.getEspecialidadesIds()));
        }

        Trabajador trabajador = Trabajador.builder()
                .dni(dto.getDni())
                .nombreCompleto(dto.getNombreCompleto())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                // --- MAPEO DE NUEVOS CAMPOS ---
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .colegiatura(dto.getColegiatura())
                .especialidades(especialidades)
                .build();

        trabajador = trabajadorRepository.save(trabajador);
        return mapToDTO(trabajador);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Trabajador trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado"));
        
        // Si está en true lo pasa a false, y si está en false lo pasa a true
        trabajador.setActivo(!trabajador.isActivo()); 
        
        trabajadorRepository.save(trabajador);
    }

    public List<TrabajadorResponseDTO> listarTodos() {
        // Volvemos a usar findAll()
        return trabajadorRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TrabajadorResponseDTO> listarMedicosActivos() {
        // Buscamos ignorando mayúsculas/minúsculas para evitar errores tipográficos en BD
        return trabajadorRepository.findByRolNombreIgnoreCaseAndActivoTrue("MEDICO").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TrabajadorResponseDTO mapToDTO(Trabajador trabajador) {
        List<String> especialidadesNombres = trabajador.getEspecialidades() != null 
            ? trabajador.getEspecialidades().stream().map(Especialidad::getNombre).collect(Collectors.toList())
            : new ArrayList<>()
            ;
        return TrabajadorResponseDTO.builder()
                .id(trabajador.getId())
                .dni(trabajador.getDni())
                .nombreCompleto(trabajador.getNombreCompleto())
                .username(trabajador.getUsername())
                .email(trabajador.getEmail())
                .telefono(trabajador.getTelefono()) 
                .fechaNacimiento(trabajador.getFechaNacimiento()) 
                .colegiatura(trabajador.getColegiatura()) 
                .rolId(trabajador.getRol().getId())
                .nombreRol(trabajador.getRol().getNombre())
                .activo(trabajador.isActivo())
                .especialidades(especialidadesNombres)
                .build();
    }

    @Transactional
    public TrabajadorResponseDTO actualizar(Long id, TrabajadorRequestDTO dto) {

        Trabajador trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado"));

        // Validar que el nuevo email no esté en uso por OTRO trabajador
        if (!trabajador.getEmail().equals(dto.getEmail()) && trabajadorRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El nuevo correo ya está en uso");
        }

        // --- APLICAR TODOS LOS CAMBIOS ---
        trabajador.setDni(dto.getDni());
        trabajador.setNombreCompleto(dto.getNombreCompleto());
        trabajador.setUsername(dto.getUsername()); // Faltaba esto
        trabajador.setEmail(dto.getEmail());
        trabajador.setTelefono(dto.getTelefono());
        trabajador.setFechaNacimiento(dto.getFechaNacimiento());
        trabajador.setColegiatura(dto.getColegiatura());
        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        trabajador.setRol(rol);

        if (rol.getNombre().equalsIgnoreCase("Médico")) {
            if (dto.getColegiatura() == null || dto.getColegiatura().isBlank()) {
                throw new RuntimeException("El número de colegiatura es obligatorio para el rol Médico.");
            }
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            trabajador.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return mapToDTO(trabajadorRepository.save(trabajador));
    }
}