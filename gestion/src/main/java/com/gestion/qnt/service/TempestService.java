package com.gestion.qnt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TempestService {

    // Índices del array obs_st devuelto por el endpoint /observations/device
    private static final String[] OBS_ST_KEYS = {
        "timestamp", "wind_lull", "wind_avg", "wind_gust", "wind_direction",
        "wind_sample_interval", "station_pressure", "air_temperature",
        "relative_humidity", "brightness", "uv", "solar_radiation",
        "precip_accum_last_1hr", "precip_type",
        "lightning_strike_avg_distance", "lightning_strike_count_last_3hr",
        "battery", "report_interval",
        "local_daily_rain_accumulation", null, null, "precip_analysis_type"
    };

    private final RestClient restClient;
    private final String token;
    private final String stationId;
    private final String deviceId;

    public TempestService(
            @Value("${tempest.token}") String token,
            @Value("${tempest.station-id:217302}") String stationId,
            @Value("${tempest.device-id:}") String deviceId,
            @Value("${tempest.base-url:https://swd.weatherflow.com/swd/rest}") String baseUrl) {
        this.token     = token;
        this.stationId = stationId;
        this.deviceId  = deviceId;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getObservations() throws RestClientException {
        // Intentar endpoint de estación (devuelve obs con keys nombradas)
        Map<String, Object> stationObs = restClient.get()
                .uri("/observations/station/{id}?token={token}", stationId, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<?> obs = stationObs != null ? (List<?>) stationObs.get("obs") : null;
        if (obs != null && !obs.isEmpty()) return stationObs;

        // Fallback: endpoint de dispositivo ST (devuelve obs como arrays de índices fijos)
        if (deviceId == null || deviceId.isBlank()) return stationObs != null ? stationObs : Map.of();

        Map<String, Object> deviceObs = restClient.get()
                .uri("/observations/device/{id}?token={token}", deviceId, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (deviceObs == null) return Map.of();

        List<?> rawObs = (List<?>) deviceObs.get("obs");
        if (rawObs == null || rawObs.isEmpty()) return deviceObs;

        // Convertir array de índices a mapa con keys nombradas
        List<?> first = (List<?>) rawObs.get(0);
        Map<String, Object> named = new LinkedHashMap<>();
        for (int i = 0; i < OBS_ST_KEYS.length && i < first.size(); i++) {
            if (OBS_ST_KEYS[i] != null) named.put(OBS_ST_KEYS[i], first.get(i));
        }

        // Agregar sea_level_pressure desde summary si existe
        Object summary = deviceObs.get("summary");
        if (summary instanceof Map<?, ?> s && s.containsKey("sea_level_pressure")) {
            named.put("sea_level_pressure", s.get("sea_level_pressure"));
        }

        return Map.of(
            "obs",     List.of(named),
            "summary", summary != null ? summary : Map.of(),
            "status",  Map.of("status_code", 0, "status_message", "SUCCESS")
        );
    }

    public Map<String, Object> getForecast(double lat, double lon) throws RestClientException {
        return restClient.get()
                .uri("/better_forecast?station_id={id}&token={token}&lat={lat}&lon={lon}",
                        stationId, token, lat, lon)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> getStationInfo() throws RestClientException {
        return restClient.get()
                .uri("/stations?token={token}", token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
