package com.clinica.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleRecetaRequestDTO {

    @NotNull(message = "El medicamento es obligatorio")
    private Long medicamentoId;

    @Size(max = 100)
    private String dosis;

    @Size(max = 100)
    private String frecuencia;

    @Size(max = 100)
    private String duracion;

    @NotNull(message = "La cantidad prescrita es obligatoria")
    @Min(value = 1, message = "La cantidad prescrita debe ser mayor a 0")
    private Integer cantidadPrescrita;

    @Size(max = 50)
    private String viaAdministracion;

    private String indicaciones;
}