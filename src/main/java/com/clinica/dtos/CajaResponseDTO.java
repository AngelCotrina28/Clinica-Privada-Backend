package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CajaResponseDTO {
    private Long id;
    private String nombre;
    private String ubicacion;
    private boolean activo;
}
