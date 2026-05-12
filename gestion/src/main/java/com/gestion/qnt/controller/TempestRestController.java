package com.gestion.qnt.controller;

import com.gestion.qnt.service.TempestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
@RequestMapping("/api/qnt/v1/meteo/tempest")
@PreAuthorize("hasRole('ADMIN')")
public class TempestRestController {

    private final TempestService tempestService;

    public TempestRestController(TempestService tempestService) {
        this.tempestService = tempestService;
    }

    @GetMapping("/observations")
    public ResponseEntity<Map<String, Object>> observations() {
        try {
            return ResponseEntity.ok(tempestService.getObservations());
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo contactar la API Tempest", "detail", e.getMessage()));
        }
    }

    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> forecast(
            @RequestParam(defaultValue = "-46.64617") double lat,
            @RequestParam(defaultValue = "-67.71842") double lon) {
        try {
            return ResponseEntity.ok(tempestService.getForecast(lat, lon));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo obtener el pronóstico Tempest", "detail", e.getMessage()));
        }
    }

    @GetMapping("/station")
    public ResponseEntity<Map<String, Object>> stationInfo() {
        try {
            return ResponseEntity.ok(tempestService.getStationInfo());
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo obtener info de la estación", "detail", e.getMessage()));
        }
    }
}
