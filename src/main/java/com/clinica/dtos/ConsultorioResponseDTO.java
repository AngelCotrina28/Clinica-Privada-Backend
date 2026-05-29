package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsultorioResponseDTO {
    private Long id;
    private String nombre;
    private String numero;
    private String piso;
    private Long especialidadId;
    private String especialidad;
    private boolean activo;
}
