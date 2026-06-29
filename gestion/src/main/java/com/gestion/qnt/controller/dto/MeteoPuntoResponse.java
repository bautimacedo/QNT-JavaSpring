package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.TempestRegistro;

import java.time.Instant;

/** Punto de una serie temporal meteorológica (para gráficos). */
public record MeteoPuntoResponse(
        Instant timestamp,
        Double windAvgMs,
        Double windGustMs,
        Double windDirection,
        Double airTemperature,
        Double relativeHumidity,
        Double stationPressure,
        Double precipAccumLast1hr,
        Long lightningLast3hr
) {
    public static MeteoPuntoResponse from(TempestRegistro r) {
        return new MeteoPuntoResponse(
                r.getTimestamp(), r.getWindAvg(), r.getWindGust(), r.getWindDirection(),
                r.getAirTemperature(), r.getRelativeHumidity(), r.getStationPressure(),
                r.getPrecipAccumLast1hr(), r.getLightningStrikeCountLast3hr());
    }
}
