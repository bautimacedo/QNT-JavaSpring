package com.gestion.qnt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class TempestService {

    private final RestClient restClient;
    private final String token;
    private final String stationId;

    public TempestService(
            @Value("${tempest.token}") String token,
            @Value("${tempest.station-id:217180}") String stationId,
            @Value("${tempest.base-url:https://swd.weatherflow.com/swd/rest}") String baseUrl) {
        this.token     = token;
        this.stationId = stationId;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Map<String, Object> getObservations() throws RestClientException {
        return restClient.get()
                .uri("/observations/station/{id}?token={token}", stationId, token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
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
                .uri("/stations/{id}?token={token}", stationId, token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
