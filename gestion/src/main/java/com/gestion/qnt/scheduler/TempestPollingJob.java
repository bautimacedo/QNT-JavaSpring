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
import java.util.ArrayDeque;
import java.util.Deque;
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

    // ── Capa de notificación con histéresis (NO afecta getAptitudActual / seguridad) ──
    private static final double HISTERESIS_GUST_MS      = 1.4;  // ~5 km/h de banda muerta
    private static final int    CONFIRMACIONES_EMPEORA  = 2;    // ~2 lecturas (~2 min)
    private static final int    CONFIRMACIONES_MEJORA   = 10;   // ~10 lecturas (~10 min)
    private static final int    FLAP_MAX_TRANSICIONES   = 3;    // 3+ transiciones en la ventana → inestable
    private static final long   FLAP_VENTANA_SEG        = 15 * 60;
    private static final int    ESTABLE_CONFIRMACIONES  = 15;   // ~15 min estable para salir de modo inestable

    private Aptitud notifiedAptitud  = null;  // último estado NOTIFICADO a la gente
    private Aptitud candidato        = null;  // estado candidato esperando confirmación
    private int     candidatoCount   = 0;
    private int     estableCount     = 0;     // lecturas consecutivas en el estado notificado
    private boolean modoInestable    = false;
    private final Deque<Instant> transiciones = new ArrayDeque<>();

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

    /**
     * Capa de notificación con histéresis. NO afecta {@link #getAptitudActual()} (seguridad),
     * que siempre refleja el estado instantáneo y conservador.
     *
     * Tres capas:
     *  1. Histéresis por banda: salir de un estado peor exige que la ráfaga baje con margen.
     *  2. Confirmación temporal asimétrica: empeorar avisa rápido (2 lecturas), mejorar exige 10.
     *     Lluvia/rayos empeoran de inmediato (peligro real).
     *  3. Modo "condiciones inestables": si flapea, un solo mensaje y silencio hasta estabilizar.
     */
    private void detectarTransicion(Evaluacion eval) {
        Aptitud instant = eval.aptitud();
        // ── SEGURIDAD: estado instantáneo siempre actualizado (cancela misiones) ──
        lastAptitud.set(instant);

        // Primera lectura: fijar baseline sin notificar
        if (notifiedAptitud == null) { notifiedAptitud = instant; return; }

        // 1. Histéresis por banda sobre la ráfaga
        Aptitud objetivo = aplicarHisteresis(instant, eval.windGustMs());

        // Estado sostenido: contar estabilidad y, si veníamos inestables, anunciar estabilización
        if (objetivo == notifiedAptitud) {
            candidato = null; candidatoCount = 0;
            if (modoInestable && ++estableCount >= ESTABLE_CONFIRMACIONES) {
                modoInestable = false; estableCount = 0;
                emitirNotificacion(objetivo, eval, true);
            }
            return;
        }
        estableCount = 0;

        // 2. Confirmación temporal asimétrica
        if (objetivo != candidato) { candidato = objetivo; candidatoCount = 1; }
        else { candidatoCount++; }

        boolean empeora  = esPeor(objetivo, notifiedAptitud);
        int requeridas;
        if (empeora && causaUrgente(eval)) requeridas = 1;            // lluvia/rayos: inmediato
        else if (empeora)                  requeridas = CONFIRMACIONES_EMPEORA;
        else                               requeridas = CONFIRMACIONES_MEJORA;
        if (candidatoCount < requeridas) return;

        // Cambio confirmado
        Aptitud anterior = notifiedAptitud;
        notifiedAptitud = objetivo;
        candidato = null; candidatoCount = 0;
        registrarTransicion();

        log.info("TempestPollingJob: transición notificada {} → {}", anterior, objetivo);

        // 3. Modo inestable: si hay flapping, un solo aviso y silenciar cambios individuales
        if (esFlapping()) {
            if (!modoInestable) {
                modoInestable = true;
                notificarInestable(eval);
            }
            return;
        }
        modoInestable = false;
        emitirNotificacion(objetivo, eval, false);
    }

    /** Si el instantáneo MEJORA respecto a lo notificado, exige que la ráfaga baje con margen. */
    private Aptitud aplicarHisteresis(Aptitud instant, double gustMs) {
        if (!esPeor(notifiedAptitud, instant)) return instant;  // empeora o igual: aceptar directo
        if (gustMs < 0) return instant;                          // sin dato de ráfaga: sin banda
        if (notifiedAptitud == Aptitud.NO_VOLAR
                && gustMs > WeatherEvaluator.PREC_WIND_GUST_MAX - HISTERESIS_GUST_MS)
            return Aptitud.NO_VOLAR;
        if (notifiedAptitud == Aptitud.PRECAUCION
                && gustMs > WeatherEvaluator.APTO_WIND_GUST_MAX - HISTERESIS_GUST_MS)
            return Aptitud.PRECAUCION;
        return instant;
    }

    /** a es peor (más restrictivo) que b. Orden enum: APTO < PRECAUCION < NO_VOLAR. */
    private static boolean esPeor(Aptitud a, Aptitud b) {
        return a.ordinal() > b.ordinal();
    }

    private static boolean causaUrgente(Evaluacion eval) {
        return eval.razones().stream().anyMatch(r -> {
            String s = r.toLowerCase();
            return s.contains("lluvia") || s.contains("éctrica") || s.contains("strike");
        });
    }

    private void registrarTransicion() {
        Instant ahora = Instant.now();
        transiciones.addLast(ahora);
        Instant corte = ahora.minusSeconds(FLAP_VENTANA_SEG);
        while (!transiciones.isEmpty() && transiciones.peekFirst().isBefore(corte))
            transiciones.removeFirst();
    }

    private boolean esFlapping() {
        return transiciones.size() >= FLAP_MAX_TRANSICIONES;
    }

    private void notificarInestable(Evaluacion eval) {
        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);
        String detalle = eval.windGustMs() >= 0
                ? String.format(" (ráfaga ~%.0f km/h, oscilando en el límite)", eval.windGustMs() * 3.6)
                : "";
        telegram.notifyAll(
            "⚠️ <b>Estación Tempest — Condiciones marginales</b>\n" +
            "<i>" + hora + " hs</i>\n\n" +
            "El viento está oscilando alrededor del límite" + detalle + ".\n" +
            "No se recomienda operar. Pausamos las alertas hasta que se estabilice."
        );
        crearAlerta(NivelAlerta.ADVERTENCIA, "⚠️ Condiciones marginales — " + hora,
                "Viento oscilando en el límite de seguridad");
    }

    private void emitirNotificacion(Aptitud aptitud, Evaluacion eval, boolean estabilizado) {
        String hora = LocalDateTime.now(ARGENTINA).format(TIME_FMT);
        String prefijo = estabilizado ? "Condiciones estabilizadas — " : "";
        String razones = String.join(", ", eval.razones());

        if (aptitud == Aptitud.NO_VOLAR) {
            telegram.notifyAll(
                "🔴 <b>Estación Tempest — " + prefijo + "NO VOLAR</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones + "\n\n" +
                "Las misiones que intenten lanzarse serán canceladas automáticamente."
            );
            avisarMisionesProgramadas();
            crearAlerta(NivelAlerta.CRITICA, "🔴 Condiciones NO VOLAR — " + hora, razones);

        } else if (aptitud == Aptitud.PRECAUCION) {
            telegram.notifyAll(
                "🟡 <b>Estación Tempest — " + prefijo + "PRECAUCIÓN</b>\n" +
                "<i>" + hora + " hs</i>\n\n" +
                razones
            );
            crearAlerta(NivelAlerta.ADVERTENCIA, "🟡 Condiciones PRECAUCIÓN — " + hora, razones);

        } else { // APTO
            telegram.notifyAll(
                "🟢 <b>Estación Tempest — " + prefijo + "APTO para volar</b>\n" +
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
