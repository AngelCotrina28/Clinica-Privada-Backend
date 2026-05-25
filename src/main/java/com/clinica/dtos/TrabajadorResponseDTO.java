package com.clinica.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
    
    private Long rolId; 
    private String nombreRol;
    
    private boolean activo;

    private List<String> especialidades;
}