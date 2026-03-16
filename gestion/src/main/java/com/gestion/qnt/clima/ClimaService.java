package com.gestion.qnt.clima;

import com.gestion.qnt.model.ClimaRegistro;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.repository.ClimaRegistroRepository;
import com.gestion.qnt.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class ClimaService {

    private static final Logger log = LoggerFactory.getLogger(ClimaService.class);

    // ── Límites de seguridad (NQN Petrol) ──────────────────────────────────────
    private static final double MAX_WIND_SPEED_MS = 100.0;
    private static final int    MIN_VISIBILITY_M  = 500;
    private static final Set<String> BAD_CONDITIONS = Set.of(
        "Rain", "Thunderstorm", "Snow", "Drizzle", "Tornado", "Squall"
    );

    private final ClimaProperties          props;
    private final ClimaRegistroRepository  registroRepo;
    private final SiteRepository           siteRepo;
    private final RestTemplate             restTemplate;

    public ClimaService(ClimaProperties props,
                        ClimaRegistroRepository registroRepo,
                        SiteRepository siteRepo) {
        this.props        = props;
        this.registroRepo = registroRepo;
        this.siteRepo     = siteRepo;
        this.restTemplate = new RestTemplate();
    }

    // ── Scheduled: cada 5 minutos ───────────────────────────────────────────────
    @Scheduled(fixedRate = 300_000, initialDelay = 5_000)
    public void fetchAll() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("[Clima] OWM_API_KEY no configurada, saltando fetch.");
            return;
        }
        for (ClimaProperties.SiteConfig cfg : props.getSites()) {
            Optional<Site> siteOpt = siteRepo.findByCodigo(cfg.getCode());
            if (siteOpt.isEmpty()) {
                log.warn("[Clima] Site con código '{}' no encontrado en BD. Saltando.", cfg.getCode());
                continue;
            }
            try {
                String url = String.format(
                    "%s?lat=%s&lon=%s&appid=%s&units=metric&lang=es",
                    props.getUrl(), cfg.getLat(), cfg.getLon(), props.getApiKey()
                );
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                ClimaRegistro reg = parse(siteOpt.get(), resp);
                registroRepo.save(reg);
                log.debug("[Clima] {} guardado — {}°C, flyable={}", cfg.getCode(),
                    reg.getTempCelsius(), reg.getIsFlyable());
            } catch (Exception e) {
                log.error("[Clima] Error fetching {}: {}", cfg.getCode(), e.getMessage());
            }
        }
    }

    // ── Consultas ───────────────────────────────────────────────────────────────
    public List<ClimaRegistro> getLatestAll() {
        return registroRepo.findLatestPerSite();
    }

    public Optional<ClimaRegistro> getLatestBySite(String codigo) {
        return registroRepo.findLatestByCodigo(codigo);
    }

    public List<ClimaRegistro> getHistorial(String codigo, int limit) {
        return registroRepo.findHistorialByCodigo(codigo, limit);
    }

    // ── Mapeo OWM → entidad ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private ClimaRegistro parse(Site site, Map<String, Object> data) {
        Map<String, Object> main  = (Map<String, Object>) data.get("main");
        Map<String, Object> wind  = (Map<String, Object>) data.get("wind");
        List<Map<String, Object>> weather = (List<Map<String, Object>>) data.get("weather");

        double windSpeed = toDouble(wind.get("speed"));
        double windGust  = wind.containsKey("gust") ? toDouble(wind.get("gust")) : 0.0;
        int    visibility = data.containsKey("visibility")
            ? ((Number) data.get("visibility")).intValue() : 10000;
        String condMain  = weather != null && !weather.isEmpty()
            ? (String) weather.get(0).get("main") : "";
        String condDesc  = weather != null && !weather.isEmpty()
            ? (String) weather.get(0).get("description") : "";

        boolean isFlyable = windSpeed <= MAX_WIND_SPEED_MS
            && !BAD_CONDITIONS.contains(condMain)
            && visibility >= MIN_VISIBILITY_M;

        ClimaRegistro reg = new ClimaRegistro();
        reg.setSite(site);
        reg.setCityName((String) data.get("name"));
        reg.setTempCelsius(toDouble(main.get("temp")));
        reg.setWindSpeedMs(windSpeed);
        reg.setWindGustMs(windGust);
        reg.setVisibilityMeters(visibility);
        reg.setConditionMain(condMain);
        reg.setConditionDesc(condDesc);
        reg.setIsFlyable(isFlyable);
        reg.setRecordedAt(Instant.now());
        return reg;
    }

    private double toDouble(Object val) {
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }
}
