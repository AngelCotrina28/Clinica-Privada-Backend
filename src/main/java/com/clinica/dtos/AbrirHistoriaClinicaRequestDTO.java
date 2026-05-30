package com.clinica.dtos;
 
import jakarta.validation.constraints.*;
import lombok.*;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbrirHistoriaClinicaRequestDTO {
 
    @NotBlank(message = "El DNI o CE del paciente es obligatorio")
    @Size(max = 9, message = "El DNI/CE no puede superar 9 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El DNI/CE solo puede contener letras y numeros")
    private String dniPaciente;
 
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombreCompleto;
 
    @Size(max = 15)
    private String telefono;
 
    @Email(message = "Formato de email inválido")
    @Size(max = 150)
    private String email;
 
    private String fechaNacimiento;
    
    private String genero;

    private String direccion;
    @Builder.Default
    private boolean desdeAdmision = false;
}
