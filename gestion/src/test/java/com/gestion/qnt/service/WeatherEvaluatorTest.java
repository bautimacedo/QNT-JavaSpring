package com.gestion.qnt.service;

import com.gestion.qnt.service.WeatherEvaluator.Aptitud;
import com.gestion.qnt.service.WeatherEvaluator.Evaluacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherEvaluatorTest {

    private final WeatherEvaluator ev = new WeatherEvaluator();

    // ── null / datos vacíos ───────────────────────────────────────────

    @Test
    void evaluar_null_retornaNull() {
        assertThat(ev.evaluar(null)).isNull();
    }

    @Test
    void evaluar_mapaVacio_retornaNull() {
        assertThat(ev.evaluar(Map.of())).isNull();
    }

    @Test
    void evaluar_obsVacio_retornaNull() {
        assertThat(ev.evaluar(Map.of("obs", List.of()))).isNull();
    }

    // ── APTO ─────────────────────────────────────────────────────────

    @Test
    void evaluar_vientoCalmo_esApto() {
        // wind_avg 10 km/h (2.7 m/s), gust 25 km/h (6.9 m/s)
        Evaluacion e = ev.evaluar(obs(2.7, 6.9, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.APTO);
        assertThat(e.razones()).isEmpty();
    }

    @Test
    void evaluar_exactoLimiteSuperiorApto_esApto() {
        // wind_avg exactamente 20 km/h (5.5 m/s), gust exactamente 35 km/h (9.7 m/s)
        Evaluacion e = ev.evaluar(obs(5.5, 9.7, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.APTO);
    }

    // ── PRECAUCIÓN ───────────────────────────────────────────────────

    @Test
    void evaluar_vientoMedio_esPrecaucion() {
        // wind_avg 30 km/h (8.3 m/s), gust 40 km/h (11.1 m/s)
        Evaluacion e = ev.evaluar(obs(8.3, 11.1, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.PRECAUCION);
    }

    @Test
    void evaluar_soloRafagaEnPrecaucion_esPrecaucion() {
        // avg calmo pero ráfaga en zona precaución
        Evaluacion e = ev.evaluar(obs(3.0, 11.0, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.PRECAUCION);
    }

    @Test
    void evaluar_lluviaLeve_esPrecaucion() {
        Evaluacion e = ev.evaluar(obs(2.0, 5.0, 1.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.PRECAUCION);
    }

    // ── NO VOLAR ─────────────────────────────────────────────────────

    @Test
    void evaluar_vientoAlto_esNoVolar() {
        // wind_avg 50 km/h (13.9 m/s)
        Evaluacion e = ev.evaluar(obs(13.9, 10.0, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.NO_VOLAR);
        assertThat(e.razones()).anyMatch(r -> r.contains("Viento"));
    }

    @Test
    void evaluar_rafagaAlta_esNoVolar() {
        // gust 55 km/h (15.3 m/s), avg calmo
        Evaluacion e = ev.evaluar(obs(3.0, 15.3, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.NO_VOLAR);
        assertThat(e.razones()).anyMatch(r -> r.contains("Ráfaga"));
    }

    @Test
    void evaluar_lluviaFuerte_esNoVolar() {
        Evaluacion e = ev.evaluar(obs(2.0, 5.0, 3.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.NO_VOLAR);
        assertThat(e.razones()).anyMatch(r -> r.contains("Lluvia"));
    }

    @Test
    void evaluar_1strike_esPrecaucion() {
        Evaluacion e = ev.evaluar(obs(2.0, 5.0, 0.0, 1L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.PRECAUCION);
        assertThat(e.razones()).anyMatch(r -> r.contains("eléctrica") || r.contains("interferencia"));
    }

    @Test
    void evaluar_3strikes_esNoVolar() {
        Evaluacion e = ev.evaluar(obs(2.0, 5.0, 0.0, 3L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.NO_VOLAR);
        assertThat(e.razones()).anyMatch(r -> r.contains("eléctrica"));
    }

    @Test
    void evaluar_rafagaExactamenteEnLimite_esNoVolar() {
        // 50.1 km/h = 13.92 m/s — justo encima del límite
        Evaluacion e = ev.evaluar(obs(3.0, 13.92, 0.0, 0L));
        assertThat(e.aptitud()).isEqualTo(Aptitud.NO_VOLAR);
    }

    // ── viento dock ──────────────────────────────────────────────────

    @Test
    void evaluarVientoDock_calmo_esApto() {
        assertThat(ev.evaluarVientoDock(5.0).aptitud()).isEqualTo(Aptitud.APTO);
    }

    @Test
    void evaluarVientoDock_alto_esNoVolar() {
        assertThat(ev.evaluarVientoDock(15.0).aptitud()).isEqualTo(Aptitud.NO_VOLAR);
    }

    // ── helper ───────────────────────────────────────────────────────

    private Map<String, Object> obs(double windAvg, double windGust, double precip, long lightning) {
        return Map.of("obs", List.of(Map.of(
                "wind_avg",  windAvg,
                "wind_gust", windGust,
                "precip_accum_last_1hr", precip,
                "lightning_strike_count_last_3hr", lightning
        )));
    }
}
