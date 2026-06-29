package com.gestion.qnt.controller;

import com.gestion.qnt.clima.TempestProperties.SiteConfig;
import com.gestion.qnt.controller.dto.AreaMeteoResponse;
import com.gestion.qnt.controller.dto.MeteoActualResponse;
import com.gestion.qnt.controller.dto.MeteoPuntoResponse;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.repository.SiteRepository;
import com.gestion.qnt.repository.TempestRegistroRepository;
import com.gestion.qnt.service.TempestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * API meteorológica PÚBLICA (sin autenticación) — alimenta la página /weather.
 * Solo lectura, datos meteorológicos no sensibles. Multi-estación.
 */
@RestController
@RequestMapping("/api/qnt/v1/meteo/publico")
public class MeteoPublicoRestController {

    private static final Set<String> GRANS = Set.of("raw", "hour", "day");

    private final TempestService tempestService;
    private final TempestRegistroRepository registroRepo;
    private final SiteRepository siteRepo;

    public MeteoPublicoRestController(TempestService tempestService,
                                      TempestRegistroRepository registroRepo,
                                      SiteRepository siteRepo) {
        this.tempestService = tempestService;
        this.registroRepo   = registroRepo;
        this.siteRepo       = siteRepo;
    }

    /** Lista de áreas con estación + su última lectura. */
    @GetMapping("/areas")
    @Transactional(readOnly = true)
    public List<AreaMeteoResponse> areas() {
        List<AreaMeteoResponse> out = new ArrayList<>();
        for (SiteConfig cfg : tempestService.getSites()) {
            Site site = siteRepo.findByCodigo(cfg.getCode()).orElse(null);
            MeteoActualResponse actual = site != null
                    ? MeteoActualResponse.from(registroRepo.findFirstBySiteOrderByTimestampDesc(site))
                    : null;
            String nombre = cfg.getName() != null && !cfg.getName().isBlank() ? cfg.getName() : cfg.getCode();
            out.add(new AreaMeteoResponse(cfg.getCode(), nombre, cfg.getLat(), cfg.getLon(), actual));
        }
        return out;
    }

    /** Última lectura de un área. */
    @GetMapping("/{code}/actual")
    @Transactional(readOnly = true)
    public ResponseEntity<MeteoActualResponse> actual(@PathVariable String code) {
        Site site = siteRepo.findByCodigo(code).orElse(null);
        if (site == null) return ResponseEntity.notFound().build();
        MeteoActualResponse r = MeteoActualResponse.from(registroRepo.findFirstBySiteOrderByTimestampDesc(site));
        return r != null ? ResponseEntity.ok(r) : ResponseEntity.noContent().build();
    }

    /** Serie temporal. gran = raw | hour | day. */
    @GetMapping("/{code}/historial")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MeteoPuntoResponse>> historial(
            @PathVariable String code,
            @RequestParam(defaultValue = "24") int horas,
            @RequestParam(defaultValue = "raw") String gran) {
        Site site = siteRepo.findByCodigo(code).orElse(null);
        if (site == null) return ResponseEntity.notFound().build();
        if (!GRANS.contains(gran)) gran = "raw";

        Instant desde = Instant.now().minusSeconds((long) horas * 3600);
        Instant hasta = Instant.now();

        if ("raw".equals(gran)) {
            List<MeteoPuntoResponse> puntos = registroRepo
                    .findBySiteAndTimestampAfterOrderByTimestampAsc(site, desde)
                    .stream().map(MeteoPuntoResponse::from).toList();
            return ResponseEntity.ok(puntos);
        }

        // Agregado (hour/day) para rangos largos — downsampling en la BD
        List<Object[]> filas = registroRepo.serieAgregada(site.getId(), desde, hasta, gran);
        List<MeteoPuntoResponse> puntos = new ArrayList<>();
        for (Object[] row : filas) {
            Instant ts = row[0] instanceof Timestamp t ? t.toInstant()
                       : row[0] instanceof Instant i ? i : null;
            puntos.add(new MeteoPuntoResponse(
                    ts, d(row[1]), d(row[2]), null, d(row[3]), d(row[5]), d(row[4]), d(row[6]), l(row[7])));
        }
        return ResponseEntity.ok(puntos);
    }

    /** Pronóstico del área (proxy a Tempest better_forecast). */
    @GetMapping("/{code}/forecast")
    public ResponseEntity<Map<String, Object>> forecast(@PathVariable String code) {
        SiteConfig cfg = tempestService.siteByCode(code);
        if (cfg == null) return ResponseEntity.notFound().build();
        try {
            return ResponseEntity.ok(tempestService.getForecast(cfg));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo obtener el pronóstico"));
        }
    }

    private static Double d(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }
    private static Long l(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }
}
