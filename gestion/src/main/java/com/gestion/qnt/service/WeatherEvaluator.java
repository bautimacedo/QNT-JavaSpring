package com.gestion.qnt.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evalúa aptitud de vuelo según datos meteorológicos del Tempest.
 * Thresholds confirmados con operaciones 13/05/2026.
 */
@Component
public class WeatherEvaluator {

    // m/s
    public static final double APTO_WIND_AVG_MAX  = 5.5;   // 20 km/h
    public static final double APTO_WIND_GUST_MAX = 9.7;   // 35 km/h
    public static final double PREC_WIND_AVG_MAX  = 12.5;  // 45 km/h
    public static final double PREC_WIND_GUST_MAX = 13.9;  // 50 km/h
    // mm/h
    public static final double PREC_RAIN_MAX      = 0.5;
    public static final double NO_FLY_RAIN_MAX    = 2.0;
    // min
    public static final long   LIGHTNING_WINDOW_MIN = 30;

    public enum Aptitud { APTO, PRECAUCION, NO_VOLAR }

    public record Evaluacion(Aptitud aptitud, List<String> razones, double windGustMs) {
        // Compat: constructor sin ráfaga conocida (-1 = desconocida)
        public Evaluacion(Aptitud aptitud, List<String> razones) {
            this(aptitud, razones, -1);
        }
    }

    /**
     * Evalúa a partir del mapa de observaciones devuelto por TempestService.getObservations().
     * Espera obs[0] como mapa con keys nombradas (ya normalizadas por el servicio).
     */
    /**
     * Retorna null si no hay datos suficientes para evaluar (sin sensor, API vacía).
     * El caller debe tratar null como "sin datos" — no bloquear lanzamientos.
     */
    @SuppressWarnings("unchecked")
    public Evaluacion evaluar(Map<String, Object> rawObs) {
        if (rawObs == null || rawObs.isEmpty()) return null;

        List<?> obsList = (List<?>) rawObs.get("obs");
        if (obsList == null || obsList.isEmpty()) return null;

        Object first = obsList.get(0);
        if (!(first instanceof Map)) return null;

        Map<String, Object> obs = (Map<String, Object>) first;

        double windAvg  = toDouble(obs.get("wind_avg"),  -1);
        double windGust = toDouble(obs.get("wind_gust"), -1);
        double precip   = toDouble(obs.get("precip_accum_last_1hr"), 0);
        long   lightning = toLong(obs.get("lightning_strike_count_last_3hr"), 0);

        List<String> razones = new ArrayList<>();
        boolean noVolar = false;
        boolean precaucion = false;

        // Viento promedio
        if (windAvg >= 0) {
            if (windAvg > PREC_WIND_AVG_MAX) {
                noVolar = true;
                razones.add(String.format("Viento %.1f km/h supera límite de %.0f km/h",
                        windAvg * 3.6, PREC_WIND_AVG_MAX * 3.6));
            } else if (windAvg > APTO_WIND_AVG_MAX) {
                precaucion = true;
                razones.add(String.format("Viento %.1f km/h (precaución)", windAvg * 3.6));
            }
        }

        // Ráfaga
        if (windGust >= 0) {
            if (windGust > PREC_WIND_GUST_MAX) {
                noVolar = true;
                razones.add(String.format("Ráfaga %.1f km/h supera límite de %.0f km/h",
                        windGust * 3.6, PREC_WIND_GUST_MAX * 3.6));
            } else if (windGust > APTO_WIND_GUST_MAX) {
                precaucion = true;
                razones.add(String.format("Ráfaga %.1f km/h (precaución)", windGust * 3.6));
            }
        }

        // Lluvia
        if (precip > NO_FLY_RAIN_MAX) {
            noVolar = true;
            razones.add(String.format("Lluvia %.1f mm/h supera límite", precip));
        } else if (precip > PREC_RAIN_MAX) {
            precaucion = true;
            razones.add(String.format("Lluvia %.1f mm/h detectada", precip));
        }

        // Rayos: 3+ strikes en las últimas 3h = NO_VOLAR; 1-2 = PRECAUCIÓN (filtra falsos positivos eléctricos)
        if (lightning >= 3) {
            noVolar = true;
            razones.add("Actividad eléctrica: " + lightning + " strikes en las últimas 3 horas");
        } else if (lightning > 0) {
            precaucion = true;
            razones.add("Posible actividad eléctrica: " + lightning + " strike detectado (puede ser interferencia)");
        }

        Aptitud aptitud = noVolar ? Aptitud.NO_VOLAR : precaucion ? Aptitud.PRECAUCION : Aptitud.APTO;
        return new Evaluacion(aptitud, razones, windGust);
    }

    /** Evalúa solo viento desde el anemómetro del dock (MQTT DockSnapshot). */
    public Evaluacion evaluarVientoDock(double windSpeedMs) {
        List<String> razones = new ArrayList<>();
        if (windSpeedMs > PREC_WIND_GUST_MAX) {
            razones.add(String.format("Ráfaga en dock %.1f km/h supera límite de %.0f km/h",
                    windSpeedMs * 3.6, PREC_WIND_GUST_MAX * 3.6));
            return new Evaluacion(Aptitud.NO_VOLAR, razones, windSpeedMs);
        }
        if (windSpeedMs > APTO_WIND_GUST_MAX) {
            razones.add(String.format("Viento en dock %.1f km/h (precaución)", windSpeedMs * 3.6));
            return new Evaluacion(Aptitud.PRECAUCION, razones, windSpeedMs);
        }
        return new Evaluacion(Aptitud.APTO, razones, windSpeedMs);
    }

    private double toDouble(Object val, double def) {
        if (val == null) return def;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return def; }
    }

    private long toLong(Object val, long def) {
        if (val == null) return def;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return def; }
    }
}
