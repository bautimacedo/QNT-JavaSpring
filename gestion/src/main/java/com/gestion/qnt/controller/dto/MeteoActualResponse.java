package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.TempestRegistro;

import java.time.Instant;

/** Lectura meteorológica actual de una estación (todas las variables). */
public record MeteoActualResponse(
        Instant timestamp,
        Double windAvgMs,
        Double windGustMs,
        Double windDirection,
        Double airTemperature,
        Double relativeHumidity,
        Double stationPressure,
        Double seaLevelPressure,
        Double uv,
        Double solarRadiation,
        Double precipAccumLast1hr,
        Long lightningLast3hr,
        Double battery,
        String aptitud
) {
    public static MeteoActualResponse from(TempestRegistro r) {
        if (r == null) return null;
        return new MeteoActualResponse(
                r.getTimestamp(), r.getWindAvg(), r.getWindGust(), r.getWindDirection(),
                r.getAirTemperature(), r.getRelativeHumidity(), r.getStationPressure(), r.getSeaLevelPressure(),
                r.getUv(), r.getSolarRadiation(), r.getPrecipAccumLast1hr(), r.getLightningStrikeCountLast3hr(),
                r.getBattery(), r.getAptitud());
    }
}
