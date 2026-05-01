package com.clinica.dtos;
 
import jakarta.validation.constraints.*;
import lombok.*;
 
/**
 * DTO de entrada para abrir una nueva Historia Clínica.
 * Roles permitidos: RECEPCIONISTA, ENFERMERO, ADMINISTRADOR, JEFE_ENFERMERIA
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbrirHistoriaClinicaRequestDTO {
 
    @NotBlank(message = "El DNI o CE del paciente es obligatorio")
    @Size(min = 8, max = 12, message = "El DNI/CE debe tener entre 8 y 12 caracteres")
    private String dniPaciente;
 
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombreCompleto;
 
    @Size(max = 15)
    private String telefono;
 
    @Email(message = "Formato de email inválido")
    @Size(max = 150)
    private String email;
 
    /** Formato: YYYY-MM-DD */
    private String fechaNacimiento;
    
    /** M / F / O */
    private String genero;

    private String direccion;
    /**
     * Indica si la historia se abre desde el módulo de Admisión y Consultas.
     * Si es true, la respuesta incluirá la URL de redirección al flujo de emergencia.
     */
    @Builder.Default
    private boolean desdeAdmision = false;
}