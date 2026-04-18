package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResponseDTO {
    private Byte id;
    private String dni;
    private String nombreCompleto;
    private String username;
    private String email;
    private String nombreRol;
    private boolean activo;
}