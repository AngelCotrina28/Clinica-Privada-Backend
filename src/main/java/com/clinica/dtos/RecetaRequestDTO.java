package com.clinica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecetaRequestDTO {

    @NotNull(message = "La atención médica es obligatoria")
    private Long atencionMedicaId;

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    private String indicacionesGenerales;

    private LocalDate fechaVencimiento;

    @NotEmpty(message = "La receta debe incluir al menos un medicamento")
    @Valid
    private List<DetalleRecetaRequestDTO> detalles;
}