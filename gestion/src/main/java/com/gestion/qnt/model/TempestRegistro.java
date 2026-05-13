package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tempest_registros", indexes = {
    @Index(name = "idx_tempest_timestamp", columnList = "timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class TempestRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "wind_avg")
    private Double windAvg;

    @Column(name = "wind_gust")
    private Double windGust;

    @Column(name = "wind_direction")
    private Double windDirection;

    @Column(name = "air_temperature")
    private Double airTemperature;

    @Column(name = "relative_humidity")
    private Double relativeHumidity;

    @Column(name = "station_pressure")
    private Double stationPressure;

    @Column(name = "sea_level_pressure")
    private Double seaLevelPressure;

    @Column(name = "uv")
    private Double uv;

    @Column(name = "solar_radiation")
    private Double solarRadiation;

    @Column(name = "precip_accum_last_1hr")
    private Double precipAccumLast1hr;

    @Column(name = "lightning_strike_count_last_3hr")
    private Long lightningStrikeCountLast3hr;

    @Column(name = "battery")
    private Double battery;

    /** Aptitud evaluada en el momento del registro: APTO, PRECAUCION, NO_VOLAR */
    @Column(name = "aptitud", length = 15)
    private String aptitud;
}
