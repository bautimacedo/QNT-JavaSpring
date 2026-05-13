package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.TempestRegistro;
import com.gestion.qnt.model.enums.NivelAlerta;
import com.gestion.qnt.model.enums.TipoAlerta;
import com.gestion.qnt.repository.AlertaRepository;
import com.gestion.qnt.repository.MisionRepository;
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

    private final TempestService tempestService;
    private final WeatherEvaluator evaluator;
    private final TempestRegistroRepository registroRepo;
    private final AlertaRepository alertaRepo;
    private final MisionRepository misionRepo;
    private final TelegramNotificationService telegram;

    private final AtomicReference<Aptitud> lastAptitud = new AtomicReference<>(null);

    public TempestPollingJob(
            TempestService tempestService,
            WeatherEvaluator evaluator,
            TempestRegistroRepository registroRepo,
            AlertaRepository alertaRepo,
            MisionRepository misionRepo,
            TelegramNotificationService telegram) {
        this.tempestService = tempestService;
        this.evaluator      = evaluator;
        this.registroRepo   = registroRepo;
        this.alertaRepo     = alertaRepo;
        this.misionRepo     = misionRepo;
        this.telegram       = telegram;
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
        } catch (Exception e) {
            log.warn("TempestPollingJob: error persistiendo registro: {}", e.getMessage());
        }
    }

    private void detectarTransicion(Evaluacion eval) {
        Aptitud anterior = lastAptitud.getAndSet(eval.aptitud());
        if (anterior == null || anterior == eval.aptitud()) return;

        log.info("TempestPollingJob: transición {} → {}", anterior, eval.aptitud());

        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);

        if (eval.aptitud() == Aptitud.NO_VOLAR) {
            String razones = String.join(", ", eval.razones());
            crearAlerta(
                NivelAlerta.CRITICA,
                "🔴 Condiciones NO VOLAR — " + hora,
                razones
            );
            telegram.notifyAll(
                "🔴 <b>Estación Tempest — NO VOLAR</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones + "\n\n" +
                "Las misiones que intenten lanzarse serán canceladas automáticamente."
            );
            avisarMisionesProgramadas();

        } else if (eval.aptitud() == Aptitud.PRECAUCION) {
            String razones = String.join(", ", eval.razones());
            crearAlerta(
                NivelAlerta.ADVERTENCIA,
                "🟡 Condiciones PRECAUCIÓN — " + hora,
                razones
            );
            telegram.notifyAll(
                "🟡 <b>Estación Tempest — PRECAUCIÓN</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones
            );

        } else if (eval.aptitud() == Aptitud.APTO && anterior != null) {
            resolverAlertasMalTiempo();
            telegram.notifyAll(
                "🟢 <b>Estación Tempest — APTO para volar</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                "Las condiciones mejoraron. Podés reprogramar misiones."
            );
        }
    }

    private void crearAlerta(NivelAlerta nivel, String mensaje, String subtitulo) {
        String dedup = "MAL_TIEMPO_TEMPEST_" + java.time.LocalDate.now(ARGENTINA);
        if (alertaRepo.existsByClaveDedup(dedup)) {
            // Actualizar la existente si empeoró a CRITICA
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
