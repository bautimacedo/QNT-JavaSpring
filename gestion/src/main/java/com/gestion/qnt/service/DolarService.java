package com.gestion.qnt.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class DolarService {

    private static final String BLUELYTICS_URL = "https://api.bluelytics.com.ar/v2/latest";

    private final RestClient restClient = RestClient.builder()
            .baseUrl(BLUELYTICS_URL)
            .build();

    /**
     * Retorna la cotización oficial de venta del dólar desde bluelytics.com.ar.
     * @return valor de venta oficial, o null si no se puede obtener
     */
    @SuppressWarnings("unchecked")
    public BigDecimal getCotizacionOficialVenta() {
        try {
            Map<String, Object> response = restClient.get()
                    .retrieve()
                    .body(Map.class);
            if (response == null) return null;
            Map<String, Object> oficial = (Map<String, Object>) response.get("oficial");
            if (oficial == null) return null;
            Object valueSell = oficial.get("value_sell");
            if (valueSell == null) return null;
            return new BigDecimal(valueSell.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
