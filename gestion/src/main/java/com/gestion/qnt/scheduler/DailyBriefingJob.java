package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.Mision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.repository.MisionRepository;
import com.gestion.qnt.service.TelegramNotificationService;
import com.gestion.qnt.service.TempestService;
import com.gestion.qnt.service.WeatherEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class DailyBriefingJob {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefingJob.class);
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE dd/MM", java.util.Locale.forLanguageTag("es-AR"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TempestService tempestService;
    private final WeatherEvaluator evaluator;
    private final MisionRepository misionRepository;
    private final TelegramNotificationService telegram;

    @Value("${owm.sites[1].lat:-46.64617}")
    private double clLat;

    @Value("${owm.sites[1].lon:-67.71842}")
    private double clLon;

    public DailyBriefingJob(TempestService tempestService, WeatherEvaluator evaluator,
                             MisionRepository misionRepository, TelegramNotificationService telegram) {
        this.tempestService  = tempestService;
        this.evaluator       = evaluator;
        this.misionRepository = misionRepository;
        this.telegram        = telegram;
    }

    /** 07:00 ART = 10:00 UTC */
    @Scheduled(cron = "0 0 10 * * *")
    public void enviarBriefingDiario() {
        LocalDate hoy = LocalDate.now(ARGENTINA);
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin    = hoy.plusDays(1).atStartOfDay();

        List<Mision> misionesHoy = misionRepository.findPlanificadasDelDia(EstadoMision.PLANIFICADA, inicio, fin);

        // Solo enviar si hay misiones planificadas en CL
        List<Mision> misionesCL = misionesHoy.stream()
                .filter(m -> m.getDron() != null
                        && m.getDron().getYacimiento() == com.gestion.qnt.model.enums.Yacimiento.CANADON_LEON)
                .toList();

        if (misionesCL.isEmpty()) {
            log.debug("DailyBriefingJob: sin misiones CL para hoy, no se envía briefing");
            return;
        }

        String pronostico = obtenerPronostico();
        String listaMisiones = formatearMisiones(misionesCL);
        String fecha = LocalDateTime.now(ARGENTINA).format(DATE_FMT);
        fecha = Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1);

        String msg = "☀️ <b>Pronóstico Cañadón León — " + fecha + "</b>\n\n"
                + pronostico + "\n\n"
                + "📋 <b>Misiones del día (" + misionesCL.size() + "):</b>\n"
                + listaMisiones + "\n\n"
                + "<i>⚠️ Pronóstico orientativo. Verificar semáforo antes de lanzar.</i>";

        telegram.notifyAll(msg);
        log.info("DailyBriefingJob: briefing enviado para {} — {} misiones CL", fecha, misionesCL.size());
    }

    @SuppressWarnings("unchecked")
    private String obtenerPronostico() {
        try {
            Map<String, Object> resp = tempestService.getForecast(clLat, clLon);
            Object forecastObj = resp.get("forecast");
            if (!(forecastObj instanceof Map)) return "🔸 Pronóstico no disponible";
            Map<String, Object> forecast = (Map<String, Object>) forecastObj;

            List<?> daily = (List<?>) forecast.get("daily");
            if (daily == null || daily.isEmpty()) return "🔸 Pronóstico no disponible";
            if (!(daily.get(0) instanceof Map)) return "🔸 Pronóstico no disponible";

            Map<String, Object> hoy = (Map<String, Object>) daily.get(0);

            double windAvgMs   = toDouble(hoy.get("wind_avg"));
            double windGustMs  = toDouble(hoy.get("wind_gust"));
            double tempMax     = toDouble(hoy.get("air_temp_high"));
            double tempMin     = toDouble(hoy.get("air_temp_low"));
            int precipProb     = toInt(hoy.get("precip_probability"));
            String conditions  = hoy.getOrDefault("conditions", "").toString();
            String icon        = hoy.getOrDefault("icon", "").toString();

            double windAvgKmh  = windAvgMs  * 3.6;
            double windGustKmh = windGustMs * 3.6;

            WeatherEvaluator.Evaluacion eval = evaluator.evaluarVientoDock(windGustMs);
            String semaforo = switch (eval.aptitud()) {
                case APTO      -> "🟢 <b>APTO para volar</b>";
                case PRECAUCION -> "🟡 <b>PRECAUCIÓN</b>";
                case NO_VOLAR  -> "🔴 <b>NO VOLAR</b>";
            };
            String weatherIcon = icon.contains("rain") ? "🌧️" : icon.contains("cloud") ? "⛅" : icon.contains("thunder") ? "⛈️" : "☀️";

            return semaforo + "\n\n"
                    + "💨 Viento esperado: " + Math.round(windAvgKmh) + " km/h | Ráfaga máx: " + Math.round(windGustKmh) + " km/h\n"
                    + "🌡️ Temperatura: " + Math.round(tempMin) + "°C – " + Math.round(tempMax) + "°C\n"
                    + "🌧️ Prob. lluvia: " + precipProb + "%\n"
                    + weatherIcon + " Condición: " + conditions;

        } catch (Exception e) {
            log.warn("DailyBriefingJob: no se pudo obtener pronóstico: {}", e.getMessage());
            return "🔸 Pronóstico no disponible (" + e.getMessage() + ")";
        }
    }

    private String formatearMisiones(List<Mision> misiones) {
        StringBuilder sb = new StringBuilder();
        for (Mision m : misiones) {
            String hora = m.getFechaProgramada() != null
                    ? m.getFechaProgramada().format(TIME_FMT) + " hs"
                    : "hora no definida";
            String dron = m.getDron() != null ? m.getDron().getNombre() : "drone sin asignar";
            sb.append("   • ").append(hora).append(" — ").append(m.getNombre())
              .append(" (").append(dron).append(")\n");
        }
        return sb.toString().stripTrailing();
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}
