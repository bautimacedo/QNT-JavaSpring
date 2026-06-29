package com.gestion.qnt.scheduler;

import com.gestion.qnt.clima.TempestProperties.SiteConfig;
import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.ClimaRegistro;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.TempestRegistro;
import com.gestion.qnt.model.enums.NivelAlerta;
import com.gestion.qnt.model.enums.TipoAlerta;
import com.gestion.qnt.repository.AlertaRepository;
import com.gestion.qnt.repository.ClimaRegistroRepository;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.repository.SiteRepository;
import com.gestion.qnt.repository.TempestRegistroRepository;
import com.gestion.qnt.service.TelegramNotificationService;
import com.gestion.qnt.service.TempestService;
import com.gestion.qnt.service.WeatherEvaluator;
import com.gestion.qnt.service.WeatherEvaluator.Aptitud;
import com.gestion.qnt.service.WeatherEvaluator.Evaluacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TempestPollingJob {

    private static final Logger log = LoggerFactory.getLogger(TempestPollingJob.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    // Site para el weather gate de misiones (compat con getAptitudActual() sin parámetro)
    private static final String SITE_GATE = "CL";

    // ── Parámetros de histéresis (NO afectan getAptitudActual / seguridad) ──
    private static final double HISTERESIS_GUST_MS      = 1.4;  // ~5 km/h de banda muerta
    private static final int    CONFIRMACIONES_EMPEORA  = 2;
    private static final int    CONFIRMACIONES_MEJORA   = 10;
    private static final int    FLAP_MAX_TRANSICIONES   = 3;
    private static final long   FLAP_VENTANA_SEG        = 15 * 60;
    private static final int    ESTABLE_CONFIRMACIONES  = 15;

    private final TempestService tempestService;
    private final WeatherEvaluator evaluator;
    private final TempestRegistroRepository registroRepo;
    private final ClimaRegistroRepository climaRegistroRepo;
    private final SiteRepository siteRepo;
    private final AlertaRepository alertaRepo;
    private final MisionRepository misionRepo;
    private final TelegramNotificationService telegram;

    /** Estado de notificación/seguridad por código de site. */
    private final Map<String, EstadoSite> estados = new ConcurrentHashMap<>();

    private static class EstadoSite {
        Aptitud lastAptitud;            // instantáneo (seguridad)
        Evaluacion lastEvaluacion;
        Aptitud notifiedAptitud;        // último estado notificado
        Aptitud candidato;
        int candidatoCount;
        int estableCount;
        boolean modoInestable;
        final Deque<Instant> transiciones = new ArrayDeque<>();
    }

    public TempestPollingJob(
            TempestService tempestService,
            WeatherEvaluator evaluator,
            TempestRegistroRepository registroRepo,
            ClimaRegistroRepository climaRegistroRepo,
            SiteRepository siteRepo,
            AlertaRepository alertaRepo,
            MisionRepository misionRepo,
            TelegramNotificationService telegram) {
        this.tempestService    = tempestService;
        this.evaluator         = evaluator;
        this.registroRepo      = registroRepo;
        this.climaRegistroRepo = climaRegistroRepo;
        this.siteRepo          = siteRepo;
        this.alertaRepo        = alertaRepo;
        this.misionRepo        = misionRepo;
        this.telegram          = telegram;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void poll() {
        for (SiteConfig cfg : tempestService.getSites()) {
            try {
                procesarSite(cfg);
            } catch (Exception e) {
                log.warn("TempestPollingJob[{}]: error: {}", cfg.getCode(), e.getMessage());
            }
        }
    }

    private void procesarSite(SiteConfig cfg) {
        Map<String, Object> raw;
        try {
            raw = tempestService.getObservations(cfg);
        } catch (Exception e) {
            log.warn("TempestPollingJob[{}]: error obteniendo observaciones: {}", cfg.getCode(), e.getMessage());
            return;
        }
        Evaluacion eval = evaluator.evaluar(raw);
        if (eval == null) return; // sin datos: preservar estado

        EstadoSite st = estados.computeIfAbsent(cfg.getCode(), k -> new EstadoSite());
        st.lastEvaluacion = eval;
        persistirRegistro(cfg, raw, eval);
        detectarTransicion(cfg, st, eval);
    }

    /** Limpieza diaria: borra registros con más de 90 días (retención del servicio meteo). */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiar() {
        Instant corte = Instant.now().minusSeconds(90L * 24 * 3600);
        registroRepo.deleteOlderThan(corte);
        log.info("TempestPollingJob: limpieza de registros anteriores a {}", corte);
    }

    /** Aptitud instantánea del site del weather gate (CL). Para misiones. */
    public Aptitud getAptitudActual() {
        EstadoSite st = estados.get(SITE_GATE);
        return st != null ? st.lastAptitud : null;
    }

    public Evaluacion getEvaluacionActual() {
        EstadoSite st = estados.get(SITE_GATE);
        return st != null ? st.lastEvaluacion : null;
    }

    // ── persistencia ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void persistirRegistro(SiteConfig cfg, Map<String, Object> raw, Evaluacion eval) {
        try {
            List<?> obsList = (List<?>) raw.get("obs");
            if (obsList == null || obsList.isEmpty()) return;
            if (!(obsList.get(0) instanceof Map)) return;
            Map<String, Object> obs = (Map<String, Object>) obsList.get(0);

            Site site = siteRepo.findByCodigo(cfg.getCode()).orElse(null);

            TempestRegistro r = new TempestRegistro();
            r.setSite(site);
            r.setTimestamp(Instant.now());
            r.setWindAvg(toDouble(obs.get("wind_avg")));
            r.setWindGust(toDouble(obs.get("wind_gust")));
            r.setWindDirection(toDouble(obs.get("wind_direction")));
            r.setAirTemperature(toDouble(obs.get("air_temperature")));
            r.setRelativeHumidity(toDouble(obs.get("relative_humidity")));
            r.setStationPressure(toDouble(obs.get("station_pressure")));
            r.setSeaLevelPressure(toDouble(obs.get("sea_level_pressure")));
            r.setUv(toDouble(obs.get("uv")));
            r.setSolarRadiation(toDouble(obs.get("solar_radiation")));
            r.setPrecipAccumLast1hr(toDouble(obs.get("precip_accum_last_1hr")));
            r.setLightningStrikeCountLast3hr(toLong(obs.get("lightning_strike_count_last_3hr")));
            r.setBattery(toDouble(obs.get("battery")));
            r.setAptitud(eval.aptitud().name());
            registroRepo.save(r);

            if (site != null) actualizarClimaRegistro(site, obs, eval);
        } catch (Exception e) {
            log.warn("TempestPollingJob[{}]: error persistiendo registro: {}", cfg.getCode(), e.getMessage());
        }
    }

    private void actualizarClimaRegistro(Site site, Map<String, Object> obs, Evaluacion eval) {
        try {
            double windAvg  = toDouble(obs.get("wind_avg"))  != null ? toDouble(obs.get("wind_avg"))  : 0.0;
            double windGust = toDouble(obs.get("wind_gust")) != null ? toDouble(obs.get("wind_gust")) : 0.0;
            double temp     = toDouble(obs.get("air_temperature")) != null ? toDouble(obs.get("air_temperature")) : 0.0;
            double precip   = toDouble(obs.get("precip_accum_last_1hr")) != null ? toDouble(obs.get("precip_accum_last_1hr")) : 0.0;
            long lightning  = toLong(obs.get("lightning_strike_count_last_3hr")) != null ? toLong(obs.get("lightning_strike_count_last_3hr")) : 0L;

            String condMain, condDesc;
            if (lightning >= 3) {
                condMain = "Thunderstorm"; condDesc = "actividad eléctrica";
            } else if (precip > 2.0) {
                condMain = "Rain"; condDesc = "lluvia intensa";
            } else if (precip > 0.5) {
                condMain = "Drizzle"; condDesc = "lluvia leve";
            } else if (windAvg > 12.5) {
                condMain = "Wind"; condDesc = "viento fuerte";
            } else {
                condMain = "Clear"; condDesc = "cielo despejado";
            }

            ClimaRegistro reg = new ClimaRegistro();
            reg.setSite(site);
            reg.setCityName(site.getNombre());
            reg.setTempCelsius(temp);
            reg.setWindSpeedMs(windAvg);
            reg.setWindGustMs(windGust);
            reg.setVisibilityMeters(10000);
            reg.setConditionMain(condMain);
            reg.setConditionDesc(condDesc);
            reg.setIsFlyable(eval.aptitud() == Aptitud.APTO);
            reg.setRecordedAt(Instant.now());
            climaRegistroRepo.save(reg);
        } catch (Exception e) {
            log.warn("TempestPollingJob: error actualizando ClimaRegistro de {}: {}", site.getCodigo(), e.getMessage());
        }
    }

    // ── notificación con histéresis (por site) ────────────────────────────

    private void detectarTransicion(SiteConfig cfg, EstadoSite st, Evaluacion eval) {
        Aptitud instant = eval.aptitud();
        st.lastAptitud = instant; // SEGURIDAD: instantáneo

        if (st.notifiedAptitud == null) { st.notifiedAptitud = instant; return; }

        Aptitud objetivo = aplicarHisteresis(st, instant, eval.windGustMs());

        if (objetivo == st.notifiedAptitud) {
            st.candidato = null; st.candidatoCount = 0;
            if (st.modoInestable && ++st.estableCount >= ESTABLE_CONFIRMACIONES) {
                st.modoInestable = false; st.estableCount = 0;
                emitirNotificacion(cfg, st, objetivo, eval, true);
            }
            return;
        }
        st.estableCount = 0;

        if (objetivo != st.candidato) { st.candidato = objetivo; st.candidatoCount = 1; }
        else { st.candidatoCount++; }

        boolean empeora = esPeor(objetivo, st.notifiedAptitud);
        int requeridas;
        if (empeora && causaUrgente(eval)) requeridas = 1;
        else if (empeora)                  requeridas = CONFIRMACIONES_EMPEORA;
        else                               requeridas = CONFIRMACIONES_MEJORA;
        if (st.candidatoCount < requeridas) return;

        Aptitud anterior = st.notifiedAptitud;
        st.notifiedAptitud = objetivo;
        st.candidato = null; st.candidatoCount = 0;
        registrarTransicion(st);

        log.info("TempestPollingJob[{}]: transición notificada {} → {}", cfg.getCode(), anterior, objetivo);

        if (esFlapping(st)) {
            if (!st.modoInestable) {
                st.modoInestable = true;
                notificarInestable(cfg, eval);
            }
            return;
        }
        st.modoInestable = false;
        emitirNotificacion(cfg, st, objetivo, eval, false);
    }

    private Aptitud aplicarHisteresis(EstadoSite st, Aptitud instant, double gustMs) {
        if (!esPeor(st.notifiedAptitud, instant)) return instant;
        if (gustMs < 0) return instant;
        if (st.notifiedAptitud == Aptitud.NO_VOLAR
                && gustMs > WeatherEvaluator.PREC_WIND_GUST_MAX - HISTERESIS_GUST_MS)
            return Aptitud.NO_VOLAR;
        if (st.notifiedAptitud == Aptitud.PRECAUCION
                && gustMs > WeatherEvaluator.APTO_WIND_GUST_MAX - HISTERESIS_GUST_MS)
            return Aptitud.PRECAUCION;
        return instant;
    }

    private static boolean esPeor(Aptitud a, Aptitud b) {
        return a.ordinal() > b.ordinal();
    }

    private static boolean causaUrgente(Evaluacion eval) {
        return eval.razones().stream().anyMatch(r -> {
            String s = r.toLowerCase();
            return s.contains("lluvia") || s.contains("éctrica") || s.contains("strike");
        });
    }

    private void registrarTransicion(EstadoSite st) {
        Instant ahora = Instant.now();
        st.transiciones.addLast(ahora);
        Instant corte = ahora.minusSeconds(FLAP_VENTANA_SEG);
        while (!st.transiciones.isEmpty() && st.transiciones.peekFirst().isBefore(corte))
            st.transiciones.removeFirst();
    }

    private boolean esFlapping(EstadoSite st) {
        return st.transiciones.size() >= FLAP_MAX_TRANSICIONES;
    }

    private String nombre(SiteConfig cfg) {
        return cfg.getName() != null && !cfg.getName().isBlank() ? cfg.getName() : cfg.getCode();
    }

    private void notificarInestable(SiteConfig cfg, Evaluacion eval) {
        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);
        String detalle = eval.windGustMs() >= 0
                ? String.format(" (ráfaga ~%.0f km/h, oscilando en el límite)", eval.windGustMs() * 3.6)
                : "";
        telegram.notifyAll(
            "⚠️ <b>" + nombre(cfg) + " — Condiciones marginales</b>\n" +
            "<i>" + hora + " hs</i>\n\n" +
            "El viento está oscilando alrededor del límite" + detalle + ".\n" +
            "No se recomienda operar. Pausamos las alertas hasta que se estabilice."
        );
        crearAlerta(cfg, NivelAlerta.ADVERTENCIA, "⚠️ " + nombre(cfg) + " — Condiciones marginales — " + hora,
                "Viento oscilando en el límite de seguridad");
    }

    private void emitirNotificacion(SiteConfig cfg, EstadoSite st, Aptitud aptitud, Evaluacion eval, boolean estabilizado) {
        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);
        String prefijo = estabilizado ? "Condiciones estabilizadas — " : "";
        String razones = String.join(", ", eval.razones());

        if (aptitud == Aptitud.NO_VOLAR) {
            telegram.notifyAll(
                "🔴 <b>" + nombre(cfg) + " — " + prefijo + "NO VOLAR</b>\n" +
                "<i>" + hora + " hs</i>\n\n" + razones + "\n\n" +
                "Las misiones que intenten lanzarse serán canceladas automáticamente."
            );
            if (SITE_GATE.equalsIgnoreCase(cfg.getCode())) avisarMisionesProgramadas();
            crearAlerta(cfg, NivelAlerta.CRITICA, "🔴 " + nombre(cfg) + " — NO VOLAR — " + hora, razones);

        } else if (aptitud == Aptitud.PRECAUCION) {
            telegram.notifyAll(
                "🟡 <b>" + nombre(cfg) + " — " + prefijo + "PRECAUCIÓN</b>\n" +
                "<i>" + hora + " hs</i>\n\n" + razones
            );
            crearAlerta(cfg, NivelAlerta.ADVERTENCIA, "🟡 " + nombre(cfg) + " — PRECAUCIÓN — " + hora, razones);

        } else { // APTO
            telegram.notifyAll(
                "🟢 <b>" + nombre(cfg) + " — " + prefijo + "APTO para volar</b>\n" +
                "<i>" + hora + " hs</i>\n\nLas condiciones mejoraron."
            );
            resolverAlertaMalTiempo(cfg);
        }
    }

    private String dedupKey(SiteConfig cfg) {
        return "MAL_TIEMPO_TEMPEST_" + cfg.getCode() + "_" + LocalDate.now(ARGENTINA);
    }

    private void crearAlerta(SiteConfig cfg, NivelAlerta nivel, String mensaje, String subtitulo) {
        try {
            String dedup = dedupKey(cfg);
            if (alertaRepo.existsByClaveDedup(dedup)) {
                alertaRepo.findByClaveDedup(dedup).ifPresent(a -> {
                    if (nivel == NivelAlerta.CRITICA) {
                        a.setNivel(nivel);
                        a.setMensaje(mensaje);
                        a.setSubtitulo(subtitulo);
                        alertaRepo.save(a);
                    }
                });
                return;
            }
            Alerta a = new Alerta(TipoAlerta.MAL_TIEMPO, nivel, mensaje, subtitulo, "ESTACION", null);
            a.setClaveDedup(dedup);
            alertaRepo.save(a);
        } catch (Exception e) {
            log.warn("TempestPollingJob: no se pudo guardar alerta en BD: {}", e.getMessage());
        }
    }

    /** Resuelve solo la alerta de mal tiempo de ESTE site (por su clave dedup). */
    private void resolverAlertaMalTiempo(SiteConfig cfg) {
        alertaRepo.findByClaveDedup(dedupKey(cfg)).ifPresent(a -> {
            if (!Boolean.TRUE.equals(a.getResuelta())) {
                a.setResuelta(true);
                a.setFechaResolucion(LocalDateTime.now());
                alertaRepo.save(a);
            }
        });
    }

    private void avisarMisionesProgramadas() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime en2h  = ahora.plusHours(2);
            long count = misionRepo.findAll().stream()
                    .filter(m -> m.getEstado() == com.gestion.qnt.model.enums.EstadoMision.PLANIFICADA)
                    .filter(m -> m.getFechaProgramada() != null
                            && m.getFechaProgramada().isAfter(ahora)
                            && m.getFechaProgramada().isBefore(en2h))
                    .count();
            if (count > 0) {
                telegram.notifyAll(
                    "⚠️ Hay <b>" + count + " misión(es)</b> programada(s) en las próximas 2 horas. " +
                    "Si el clima no mejora serán canceladas al intentar lanzarse."
                );
            }
        } catch (Exception e) {
            log.warn("TempestPollingJob: error chequeando misiones próximas: {}", e.getMessage());
        }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
