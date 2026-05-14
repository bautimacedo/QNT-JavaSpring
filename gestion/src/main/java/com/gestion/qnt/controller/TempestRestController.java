package com.gestion.qnt.controller;

import com.gestion.qnt.model.TempestRegistro;
import com.gestion.qnt.repository.TempestRegistroRepository;
import com.gestion.qnt.service.TelegramNotificationService;
import com.gestion.qnt.service.TempestService;
import com.gestion.qnt.service.WeatherEvaluator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qnt/v1/meteo/tempest")
@PreAuthorize("hasRole('ADMIN')")
public class TempestRestController {

    private final TempestService tempestService;
    private final TempestRegistroRepository registroRepo;
    private final WeatherEvaluator weatherEvaluator;
    private final TelegramNotificationService telegramService;

    public TempestRestController(TempestService tempestService, TempestRegistroRepository registroRepo,
                                  WeatherEvaluator weatherEvaluator, TelegramNotificationService telegramService) {
        this.tempestService   = tempestService;
        this.registroRepo     = registroRepo;
        this.weatherEvaluator = weatherEvaluator;
        this.telegramService  = telegramService;
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

    @GetMapping("/historial")
    public ResponseEntity<List<TempestRegistro>> historial(
            @RequestParam(defaultValue = "24") int horas) {
        Instant desde = Instant.now().minusSeconds((long) horas * 3600);
        return ResponseEntity.ok(registroRepo.findByTimestampAfterOrderByTimestampAsc(desde));
    }

    /** Endpoint de prueba — simula una ráfaga y manda el mensaje real a Telegram. Solo ADMIN. */
    @PostMapping("/test-alerta")
    public ResponseEntity<Map<String, Object>> testAlerta(
            @RequestParam(defaultValue = "100") double rafagaKmh) {
        double gustMs = rafagaKmh / 3.6;
        WeatherEvaluator.Evaluacion eval = weatherEvaluator.evaluarVientoDock(gustMs);
        String hora = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        String msg = switch (eval.aptitud()) {
            case NO_VOLAR -> "🔴 <b>[TEST] Estación Tempest — NO VOLAR</b>\n" +
                    "<i>" + hora + " hs</i>\n\n" +
                    String.join(", ", eval.razones()) + "\n\n" +
                    "Las misiones que intenten lanzarse serán canceladas automáticamente.";
            case PRECAUCION -> "🟡 <b>[TEST] Estación Tempest — PRECAUCIÓN</b>\n" +
                    "<i>" + hora + " hs</i>\n\n" +
                    String.join(", ", eval.razones());
            case APTO -> "🟢 <b>[TEST] Estación Tempest — APTO para volar</b>\n" +
                    "<i>" + hora + " hs</i>\n\nCondiciones normales.";
        };

        telegramService.notifyAll(msg);
        return ResponseEntity.ok(Map.of(
                "aptitud", eval.aptitud().name(),
                "rafagaKmh", rafagaKmh,
                "razones", eval.razones(),
                "mensajeEnviado", msg
        ));
    }
}
