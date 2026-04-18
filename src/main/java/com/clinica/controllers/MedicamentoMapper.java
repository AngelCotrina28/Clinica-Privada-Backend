package com.clinica.controllers;

import com.clinica.dtos.MedicamentoResponseDTO;
import com.clinica.model.entities.Medicamento;

import org.springframework.stereotype.Component;

@Component
public class MedicamentoMapper {

    public MedicamentoResponseDTO toResponse(Medicamento m) {
        return MedicamentoResponseDTO.builder()
                .id(m.getId())
                .codigo(m.getCodigo())
                .nombre(m.getNombre())
                .nombreGenerico(m.getNombreGenerico())
                .descripcion(m.getDescripcion())
                .categoriaId(m.getCategoria().getId())
                .categoriaNombre(m.getCategoria().getNombre())
                .presentacion(m.getPresentacion())
                .laboratorio(m.getLaboratorio())
                .precioUnitario(m.getPrecioUnitario())
                .stockActual(m.getStockActual())
                .stockMinimo(m.getStockMinimo())
                .requiereReceta(m.isRequiereReceta())
                .activo(m.isActivo())
                .creadoPor(m.getCreatedBy() != null ? m.getCreatedBy().getUsuario() : null)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}