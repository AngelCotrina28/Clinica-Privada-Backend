package com.clinica.services;

import com.clinica.dtos.SerieComprobanteRequestDTO;
import com.clinica.dtos.SerieComprobanteResponseDTO;
import com.clinica.exceptions.RecursoNoEncontradoException;
import com.clinica.model.entities.SerieComprobante;
import com.clinica.model.repositories.SerieComprobanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SerieComprobanteService {

    private static final int LONGITUD_CORRELATIVO = 6;

    private final SerieComprobanteRepository serieRepository;

    @Transactional(readOnly = true)
    public List<SerieComprobanteResponseDTO> listarTodos() {
        return serieRepository.findAll(Sort.by("tipoComprobante", "serie")).stream()
                .filter(serie -> esTipoPermitido(serie.getTipoComprobante()))
                .map(this::map)
                .toList();
    }

    @Transactional
    public SerieComprobanteResponseDTO crear(SerieComprobanteRequestDTO request) {
        SerieComprobante.TipoComprobante tipo = validarTipoPermitido(request.getTipoComprobante());
        String prefijo = normalizarPrefijo(request.getPrefijo());
        validarPrefijoParaTipo(tipo, prefijo);
        validarPrefijoActivoUnico(prefijo, null);
        validarDuplicadoFisico(tipo, prefijo, null);

        SerieComprobante serie = SerieComprobante.builder()
                .tipoComprobante(tipo)
                .serie(prefijo)
                .correlativoActual(request.getNumeroInicial() - 1)
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        return map(serieRepository.save(serie));
    }

    @Transactional
    public SerieComprobanteResponseDTO actualizar(Long id, SerieComprobanteRequestDTO request) {
        SerieComprobante serie = serieRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Serie de comprobante no encontrada."));

        SerieComprobante.TipoComprobante tipo = validarTipoPermitido(request.getTipoComprobante());
        String prefijo = normalizarPrefijo(request.getPrefijo());
        validarPrefijoParaTipo(tipo, prefijo);
        validarPrefijoActivoUnico(prefijo, id);
        validarDuplicadoFisico(tipo, prefijo, id);

        serie.setTipoComprobante(tipo);
        serie.setSerie(prefijo);
        serie.setCorrelativoActual(request.getNumeroInicial() - 1);
        if (request.getActivo() != null) {
            serie.setActivo(request.getActivo());
        }

        return map(serieRepository.save(serie));
    }

    @Transactional
    public void cambiarEstado(Long id) {
        SerieComprobante serie = serieRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Serie de comprobante no encontrada."));

        if (!serie.isActivo()) {
            validarPrefijoActivoUnico(serie.getSerie(), serie.getId());
        }

        serie.setActivo(!serie.isActivo());
        serieRepository.save(serie);
    }

    private SerieComprobante.TipoComprobante validarTipoPermitido(SerieComprobante.TipoComprobante tipo) {
        if (!esTipoPermitido(tipo)) {
            throw new IllegalArgumentException("Solo se permiten series de BOLETA o FACTURA.");
        }
        return tipo;
    }

    private boolean esTipoPermitido(SerieComprobante.TipoComprobante tipo) {
        return tipo == SerieComprobante.TipoComprobante.BOLETA
                || tipo == SerieComprobante.TipoComprobante.FACTURA;
    }

    private String normalizarPrefijo(String prefijo) {
        return prefijo == null ? "" : prefijo.trim().toUpperCase();
    }

    private void validarPrefijoParaTipo(SerieComprobante.TipoComprobante tipo, String prefijo) {
        if (tipo == SerieComprobante.TipoComprobante.BOLETA && !prefijo.startsWith("B")) {
            throw new IllegalArgumentException("Las series de boleta deben iniciar con B. Ejemplo: B001.");
        }
        if (tipo == SerieComprobante.TipoComprobante.FACTURA && !prefijo.startsWith("F")) {
            throw new IllegalArgumentException("Las series de factura deben iniciar con F. Ejemplo: F001.");
        }
    }

    private void validarPrefijoActivoUnico(String prefijo, Long idActual) {
        boolean existe = idActual == null
                ? serieRepository.existsBySerieIgnoreCaseAndActivoTrue(prefijo)
                : serieRepository.existsBySerieIgnoreCaseAndActivoTrueAndIdNot(prefijo, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe una serie activa con el prefijo " + prefijo + ".");
        }
    }

    private void validarDuplicadoFisico(
            SerieComprobante.TipoComprobante tipo,
            String prefijo,
            Long idActual) {
        boolean existe = idActual == null
                ? serieRepository.existsByTipoComprobanteAndSerieIgnoreCase(tipo, prefijo)
                : serieRepository.existsByTipoComprobanteAndSerieIgnoreCaseAndIdNot(tipo, prefijo, idActual);
        if (existe) {
            throw new IllegalStateException(
                    "Ya existe una serie " + tipo.name() + " con el prefijo " + prefijo
                            + ". Reactivela o edite la existente.");
        }
    }

    private SerieComprobanteResponseDTO map(SerieComprobante serie) {
        int siguiente = serie.getCorrelativoActual() + 1;
        return SerieComprobanteResponseDTO.builder()
                .id(serie.getId())
                .tipoComprobante(serie.getTipoComprobante().name())
                .prefijo(serie.getSerie())
                .correlativoActual(serie.getCorrelativoActual())
                .siguienteCorrelativo(siguiente)
                .siguienteNumero(serie.getSerie() + "-" + String.format("%0" + LONGITUD_CORRELATIVO + "d", siguiente))
                .activo(serie.isActivo())
                .build();
    }
}
