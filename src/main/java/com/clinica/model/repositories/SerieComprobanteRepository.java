package com.clinica.model.repositories;

import com.clinica.model.entities.SerieComprobante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Long> {

    boolean existsBySerieIgnoreCaseAndActivoTrue(String serie);

    boolean existsBySerieIgnoreCaseAndActivoTrueAndIdNot(String serie, Long id);

    boolean existsByTipoComprobanteAndSerieIgnoreCase(
            SerieComprobante.TipoComprobante tipoComprobante,
            String serie);

    boolean existsByTipoComprobanteAndSerieIgnoreCaseAndIdNot(
            SerieComprobante.TipoComprobante tipoComprobante,
            String serie,
            Long id);
}
