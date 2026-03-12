package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "clima_registros", indexes = {
    @Index(name = "idx_clima_site_recorded", columnList = "site_id, recorded_at DESC")
})
@Getter
@Setter
public class ClimaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "temp_celsius")
    private Double tempCelsius;

    @Column(name = "wind_speed_ms")
    private Double windSpeedMs;

    @Column(name = "wind_gust_ms")
    private Double windGustMs;

    @Column(name = "visibility_meters")
    private Integer visibilityMeters;

    @Column(name = "condition_main", length = 50)
    private String conditionMain;

    @Column(name = "condition_desc", length = 100)
    private String conditionDesc;

    @Column(name = "is_flyable", nullable = false)
    private Boolean isFlyable;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
