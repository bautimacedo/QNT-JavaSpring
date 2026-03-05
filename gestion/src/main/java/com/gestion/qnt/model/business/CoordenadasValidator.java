package com.gestion.qnt.model.business;

import com.gestion.qnt.model.business.exceptions.BusinessException;

import java.math.BigDecimal;

/**
 * Validación de coordenadas para equipos en el mapa.
 */
public final class CoordenadasValidator {

    private static final BigDecimal LAT_MIN = new BigDecimal("-90");
    private static final BigDecimal LAT_MAX = new BigDecimal("90");
    private static final BigDecimal LNG_MIN = new BigDecimal("-180");
    private static final BigDecimal LNG_MAX = new BigDecimal("180");

    private CoordenadasValidator() {
    }

    /**
     * Valida latitud y longitud si no son nulas.
     * Latitud debe estar en [-90, 90], longitud en [-180, 180].
     * Altitud no se valida (cualquier valor permitido).
     */
    public static void validar(BigDecimal latitud, BigDecimal longitud) throws BusinessException {
        if (latitud != null) {
            if (latitud.compareTo(LAT_MIN) < 0 || latitud.compareTo(LAT_MAX) > 0) {
                throw new BusinessException("La latitud debe estar entre -90 y 90");
            }
        }
        if (longitud != null) {
            if (longitud.compareTo(LNG_MIN) < 0 || longitud.compareTo(LNG_MAX) > 0) {
                throw new BusinessException("La longitud debe estar entre -180 y 180");
            }
        }
    }
}
