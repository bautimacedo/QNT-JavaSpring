package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.ClimaRegistro;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TempestPollingJob {

    private static final Logger log = LoggerFactory.getLogger(TempestPollingJob.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private static final String SITE_TEMPEST = "CL";

    private final TempestService tempestService;
    private final WeatherEvaluator evaluator;
    private final TempestRegistroRepository registroRepo;
    private final ClimaRegistroRepository climaRegistroRepo;
    private final SiteRepository siteRepo;
    private final AlertaRepository alertaRepo;
    private final MisionRepository misionRepo;
    private final TelegramNotificationService telegram;

    private final AtomicReference<Aptitud>    lastAptitud    = new AtomicReference<>(null);
    private final AtomicReference<Evaluacion> lastEvaluacion = new AtomicReference<>(null);

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
        Map<String, Object> raw;
        try {
            raw = tempestService.getObservations();
        } catch (Exception e) {
            log.warn("TempestPollingJob: error obteniendo observaciones: {}", e.getMessage());
            return;
        }

        Evaluacion eval = evaluator.evaluar(raw);
        if (eval == null) {
            // Sin sensor o datos vacíos — no modificar el estado actual
            log.debug("TempestPollingJob: sin datos de observación, estado actual preservado");
            return;
        }

        lastEvaluacion.set(eval);
        persistirRegistro(raw, eval);
        detectarTransicion(eval);
    }

    /** Limpieza diaria: borra registros con más de 48 horas. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiar() {
        Instant corte = Instant.now().minusSeconds(48 * 3600);
        registroRepo.deleteOlderThan(corte);
        log.info("TempestPollingJob: limpieza de registros anteriores a {}", corte);
    }

    /** Devuelve la última aptitud conocida (para el weather gate). */
    public Aptitud getAptitudActual() {
        return lastAptitud.get();
    }

    /** Devuelve la última evaluación completa (aptitud + razones) para mensajes detallados. */
    public Evaluacion getEvaluacionActual() {
        return lastEvaluacion.get();
    }

    // ── privados ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void persistirRegistro(Map<String, Object> raw, Evaluacion eval) {
        try {
            List<?> obsList = (List<?>) raw.get("obs");
            if (obsList == null || obsList.isEmpty()) return;
            Object first = obsList.get(0);
            if (!(first instanceof Map)) return;
            Map<String, Object> obs = (Map<String, Object>) first;

            TempestRegistro r = new TempestRegistro();
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
            actualizarClimaRegistro(obs, eval);
        } catch (Exception e) {
            log.warn("TempestPollingJob: error persistiendo registro: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void actualizarClimaRegistro(Map<String, Object> obs, Evaluacion eval) {
        try {
            siteRepo.findByCodigo(SITE_TEMPEST).ifPresent(site -> {
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
                reg.setCityName("Cañadon Leon");
                reg.setTempCelsius(temp);
                reg.setWindSpeedMs(windAvg);
                reg.setWindGustMs(windGust);
                reg.setVisibilityMeters(10000);
                reg.setConditionMain(condMain);
                reg.setConditionDesc(condDesc);
                reg.setIsFlyable(eval.aptitud() == Aptitud.APTO);
                reg.setRecordedAt(Instant.now());
                climaRegistroRepo.save(reg);
                log.debug("[Tempest→Clima] CL actualizado — {}°C, viento {} m/s, flyable={}", temp, windAvg, eval.aptitud() == Aptitud.APTO);
            });
        } catch (Exception e) {
            log.warn("TempestPollingJob: error actualizando ClimaRegistro: {}", e.getMessage());
        }
    }

    private void detectarTransicion(Evaluacion eval) {
        Aptitud anterior = lastAptitud.getAndSet(eval.aptitud());
        if (anterior == null || anterior == eval.aptitud()) return;

        log.info("TempestPollingJob: transición {} → {}", anterior, eval.aptitud());

        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);

        if (eval.aptitud() == Aptitud.NO_VOLAR) {
            String razones = String.join(", ", eval.razones());
            telegram.notifyAll(
                "🔴 <b>Estación Tempest — NO VOLAR</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones + "\n\n" +
                "Las misiones que intenten lanzarse serán canceladas automáticamente."
            );
            avisarMisionesProgramadas();
            crearAlerta(NivelAlerta.CRITICA, "🔴 Condiciones NO VOLAR — " + hora, razones);

        } else if (eval.aptitud() == Aptitud.PRECAUCION) {
            String razones = String.join(", ", eval.razones());
            telegram.notifyAll(
                "🟡 <b>Estación Tempest — PRECAUCIÓN</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones
            );
            crearAlerta(NivelAlerta.ADVERTENCIA, "🟡 Condiciones PRECAUCIÓN — " + hora, razones);

        } else if (eval.aptitud() == Aptitud.APTO && anterior != null) {
            telegram.notifyAll(
                "🟢 <b>Estación Tempest — APTO para volar</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                "Las condiciones mejoraron. Podés reprogramar misiones."
            );
            resolverAlertasMalTiempo();
        }
    }

    private void crearAlerta(NivelAlerta nivel, String mensaje, String subtitulo) {
        try {
            String dedup = "MAL_TIEMPO_TEMPEST_" + java.time.LocalDate.now(ARGENTINA);
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

    private void resolverAlertasMalTiempo() {
        alertaRepo.findByTipoAndResueltaFalse(TipoAlerta.MAL_TIEMPO).forEach(a -> {
            a.setResuelta(true);
            a.setFechaResolucion(LocalDateTime.now());
            alertaRepo.save(a);
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
