package com.clinica.services;

import com.clinica.dtos.TrabajadorRequestDTO;
import com.clinica.dtos.TrabajadorResponseDTO;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.Trabajador;
import com.clinica.model.repositories.RolRepository;
import com.clinica.model.repositories.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TrabajadorResponseDTO registrar(TrabajadorRequestDTO dto) {
        // CA2: Validar correo único
        if (trabajadorRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }
        // Validar DNI único
        if (trabajadorRepository.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        Trabajador trabajador = Trabajador.builder()
                .dni(dto.getDni())
                .nombreCompleto(dto.getNombreCompleto())
                .username(dto.getUsername())
                .email(dto.getEmail())
                // CA3: Guardar contraseña encriptada
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                .build();

        trabajador = trabajadorRepository.save(trabajador);
        return mapToDTO(trabajador);
    }

    // CA4: Eliminación lógica
    @Transactional
    public void eliminarLogico(Byte id) {
        Trabajador Trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado"));
        Trabajador.setActivo(false);
        trabajadorRepository.save(Trabajador);
    }

    public List<TrabajadorResponseDTO> listarTodos() {
        return trabajadorRepository.findAllByActivoTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TrabajadorResponseDTO mapToDTO(Trabajador trabajador) {
        return TrabajadorResponseDTO.builder()
                .id(trabajador.getId())
                .dni(trabajador.getDni())
                .nombreCompleto(trabajador.getNombreCompleto())
                .username(trabajador.getUsername())
                .email(trabajador.getEmail())
                // --- NUEVOS CAMPOS ---
                .telefono(trabajador.getTelefono()) 
                .fechaNacimiento(trabajador.getFechaNacimiento()) 
                .colegiatura(trabajador.getColegiatura()) 
                // ---------------------
                .nombreRol(trabajador.getRol().getNombre()) // Obtenemos el String del rol
                .activo(trabajador.isActivo())
                .build();
    }
}