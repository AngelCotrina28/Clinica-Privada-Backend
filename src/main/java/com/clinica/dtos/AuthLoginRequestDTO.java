package com.clinica.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginRequestDTO {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 1, max = 50, message = "El usuario debe tener entre 1 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}