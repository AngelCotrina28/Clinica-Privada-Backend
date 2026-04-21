package com.clinica.services;

import com.clinica.dtos.UsuarioRequestDTO;
import com.clinica.dtos.UsuarioResponseDTO;
import com.clinica.model.entities.Rol;
import com.clinica.model.entities.Usuario;
import com.clinica.model.repositories.RolRepository;
import com.clinica.model.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO registrar(UsuarioRequestDTO dto) {
        // CA2: Validar correo único
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }
        // Validar DNI único
        if (usuarioRepository.existsByDni(dto.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        Usuario usuario = Usuario.builder()
                .dni(dto.getDni())
                .nombreCompleto(dto.getNombreCompleto())
                .username(dto.getUsername())
                .email(dto.getEmail())
                // CA3: Guardar contraseña encriptada
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        return mapToDTO(usuario);
    }

    // CA4: Eliminación lógica
    @Transactional
    public void eliminarLogico(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UsuarioResponseDTO mapToDTO(Usuario u) {
        return UsuarioResponseDTO.builder()
                .id(u.getId())
                .dni(u.getDni())
                .nombreCompleto(u.getNombreCompleto())
                .username(u.getUsername())
                .email(u.getEmail())
                .nombreRol(u.getRol().getNombre())
                .activo(u.isActivo())
                .build();
    }
}