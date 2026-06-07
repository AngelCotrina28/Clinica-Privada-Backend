package com.clinica.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CitaOpcionDTO {
    private String codigo;
    private String tipo;
    private LocalDateTime fecha;
}