package com.gestion.qnt.service;

import com.gestion.qnt.clima.TempestProperties;
import com.gestion.qnt.clima.TempestProperties.SiteConfig;
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
    private final TempestProperties props;

    public TempestService(TempestProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    /** Estaciones configuradas. */
    public List<SiteConfig> getSites() { return props.getSites(); }

    /** La primera estación configurada (default, para los métodos legacy sin parámetro). */
    public SiteConfig defaultSite() {
        return props.getSites().isEmpty() ? null : props.getSites().get(0);
    }

    public SiteConfig siteByCode(String code) {
        return props.getSites().stream()
                .filter(s -> s.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    // ── Observaciones ────────────────────────────────────────────────────────

    /** Observaciones de una estación específica. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getObservations(SiteConfig site) throws RestClientException {
        if (site == null) return Map.of();
        String token = props.getToken();

        Map<String, Object> stationObs = restClient.get()
                .uri("/observations/station/{id}?token={token}", site.getStationId(), token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<?> obs = stationObs != null ? (List<?>) stationObs.get("obs") : null;
        if (obs != null && !obs.isEmpty()) {
            corregirDireccionViento(stationObs, site.getWindOffset());
            return stationObs;
        }

        // Fallback: endpoint de dispositivo ST (obs como arrays de índices fijos)
        if (site.getDeviceId() == null || site.getDeviceId().isBlank())
            return stationObs != null ? stationObs : Map.of();

        Map<String, Object> deviceObs = restClient.get()
                .uri("/observations/device/{id}?token={token}", site.getDeviceId(), token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (deviceObs == null) return Map.of();

        List<?> rawObs = (List<?>) deviceObs.get("obs");
        if (rawObs == null || rawObs.isEmpty()) return deviceObs;

        List<?> first = (List<?>) rawObs.get(0);
        Map<String, Object> named = new LinkedHashMap<>();
        for (int i = 0; i < OBS_ST_KEYS.length && i < first.size(); i++) {
            if (OBS_ST_KEYS[i] != null) named.put(OBS_ST_KEYS[i], first.get(i));
        }

        Object summary = deviceObs.get("summary");
        if (summary instanceof Map<?, ?> s && s.containsKey("sea_level_pressure")) {
            named.put("sea_level_pressure", s.get("sea_level_pressure"));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("obs", List.of(named));
        resultado.put("summary", summary != null ? summary : Map.of());
        resultado.put("status", Map.of("status_code", 0, "status_message", "SUCCESS"));
        corregirDireccionViento(resultado, site.getWindOffset());
        return resultado;
    }

    /** Legacy: observaciones de la estación por defecto. */
    public Map<String, Object> getObservations() throws RestClientException {
        return getObservations(defaultSite());
    }

    // ── Pronóstico e info ─────────────────────────────────────────────────────

    public Map<String, Object> getForecast(double lat, double lon) throws RestClientException {
        SiteConfig def = defaultSite();
        String stationId = def != null ? def.getStationId() : "";
        return restClient.get()
                .uri("/better_forecast?station_id={id}&token={token}&lat={lat}&lon={lon}",
                        stationId, props.getToken(), lat, lon)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /** Pronóstico de una estación específica (usa su station-id y lat/lon). */
    public Map<String, Object> getForecast(SiteConfig site) throws RestClientException {
        if (site == null) return Map.of();
        return restClient.get()
                .uri("/better_forecast?station_id={id}&token={token}&lat={lat}&lon={lon}",
                        site.getStationId(), props.getToken(), site.getLat(), site.getLon())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> getStationInfo() throws RestClientException {
        return restClient.get()
                .uri("/stations?token={token}", props.getToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /** Corrige la dirección del viento por el offset físico de la veleta. */
    @SuppressWarnings("unchecked")
    private void corregirDireccionViento(Map<String, Object> resultado, double offset) {
        if (resultado == null || offset == 0) return;
        Object obsObj = resultado.get("obs");
        if (!(obsObj instanceof List<?> obs) || obs.isEmpty()) return;
        if (!(obs.get(0) instanceof Map)) return;
        Map<String, Object> m = (Map<String, Object>) obs.get(0);
        Object wd = m.get("wind_direction");
        if (wd instanceof Number n) {
            double corregida = ((n.doubleValue() + offset) % 360 + 360) % 360;
            m.put("wind_direction", corregida);
        }
    }
}
