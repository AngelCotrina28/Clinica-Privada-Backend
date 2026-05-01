package com.clinica.dtos;

import lombok.*;
import java.time.LocalDateTime;
/**
 * DTO de respuesta para Historia Clínica.
 * Incluye redirectUrl cuando la historia se crea desde el módulo de Admisión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriaClinicaResponseDTO {
 
    private Long id;
    private String numeroHistoria;
    private String dniPaciente;
    private String nombreCompleto;
    private String telefono;
    private String email;
    private String fechaNacimiento;
    private String genero;
    private String direccion;
    private String creadoPor;
    private LocalDateTime createdAt;
 
    /**
     * Solo se rellena cuando la historia se crea desde Admisión (desdeAdmision=true).
     * El frontend usa esta URL para redirigir automáticamente al flujo de emergencia.
     * Ejemplo: "/admision/emergencia?historiaId=5&nombre=Juan+Perez&dni=12345678"
     */
    private String redirectUrl;
    /** Indica si la historia ya existía (búsqueda) o fue recién creada */
    private boolean nuevaHistoria;
}