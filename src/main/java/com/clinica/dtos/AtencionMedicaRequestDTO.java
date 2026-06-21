package com.clinica.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtencionMedicaRequestDTO {

    @NotNull(message = "El ID de la historia clínica es obligatorio")
    private Long historiaClinicaId;

    @NotNull(message = "El ID del médico es obligatorio")
    private Long medicoId;

    private String numeroCita;

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnosticoPrincipal;

    private String notasEvolucion;

    private List<ItemRecetaRequestDTO> itemsReceta;
}