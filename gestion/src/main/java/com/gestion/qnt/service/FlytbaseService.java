package com.gestion.qnt.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Servicio para integración con FlytBase vía webhooks.
 * Usado actualmente para lanzar misiones del yacimiento EFO.
 */
@Service
public class FlytbaseService {

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Dispara el webhook de FlytBase para lanzar una misión.
     *
     * @param webhookUrl  URL completa del webhook (incluye el token, ej: https://api.flytbase.com/v2/integrations/webhook/xxx)
     * @param bearer      Token Bearer de autorización
     * @param descripcion Descripción/nombre de la misión (va como "description" en el body)
     * @param lat         Latitud del sitio de operación (del dock asignado)
     * @param lon         Longitud del sitio de operación (del dock asignado)
     * @throws RuntimeException si FlytBase responde con error HTTP
     */
    public void lanzarMision(String webhookUrl, String bearer,
                              String descripcion, BigDecimal lat, BigDecimal lon) {
        Map<String, Object> body = Map.of(
                "timestamp",    System.currentTimeMillis(),
                "severity",     2,
                "description",  descripcion != null ? descripcion : "Misión QNT",
                "latitude",     lat  != null ? lat.doubleValue()  : 0.0,
                "longitude",    lon  != null ? lon.doubleValue()  : 0.0,
                "altitude_msl", 100,
                "metadata",     Map.of("source", "qnt-gestion")
        );

        restClient.post()
                .uri(webhookUrl)
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
