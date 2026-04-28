package com.clinica.dtos; // Ajusta el paquete si es necesario

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorResponseDTO {
    
    private Long id;
    private String dni;
    private String nombreCompleto;
    private String username;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String colegiatura;
    
    // Solo enviamos el nombre del rol (ej. "Médico"), no todo el objeto Rol
    private Long rolId; 
    private String nombreRol;
    
    private boolean activo;
}