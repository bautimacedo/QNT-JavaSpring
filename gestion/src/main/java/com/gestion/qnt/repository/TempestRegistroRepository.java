package com.gestion.qnt.repository;

import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.TempestRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface TempestRegistroRepository extends JpaRepository<TempestRegistro, Long> {

    List<TempestRegistro> findByTimestampAfterOrderByTimestampAsc(Instant from);

    // ── Multi-estación ────────────────────────────────────────────────────────
    List<TempestRegistro> findBySiteAndTimestampAfterOrderByTimestampAsc(Site site, Instant from);

    List<TempestRegistro> findBySiteAndTimestampBetweenOrderByTimestampAsc(Site site, Instant desde, Instant hasta);

    TempestRegistro findFirstBySiteOrderByTimestampDesc(Site site);

    /**
     * Serie agregada por intervalo (downsampling) para gráficos de rangos largos.
     * granularidad = 'hour' | 'day' (date_trunc de PostgreSQL). Devuelve:
     * [bucket(Instant/Timestamp), avgWindAvg, maxWindGust, avgTemp, avgPressure, sumPrecip, maxLightning].
     */
    @Query(value = """
        SELECT date_trunc(:gran, t.timestamp) AS bucket,
               AVG(t.wind_avg)            AS wind_avg,
               MAX(t.wind_gust)           AS wind_gust,
               AVG(t.air_temperature)     AS air_temperature,
               AVG(t.station_pressure)    AS station_pressure,
               AVG(t.relative_humidity)   AS relative_humidity,
               SUM(t.precip_accum_last_1hr) AS precip,
               MAX(t.lightning_strike_count_last_3hr) AS lightning
        FROM tempest_registros t
        WHERE t.site_id = :siteId AND t.timestamp BETWEEN :desde AND :hasta
        GROUP BY bucket
        ORDER BY bucket ASC
        """, nativeQuery = true)
    List<Object[]> serieAgregada(@Param("siteId") Long siteId,
                                 @Param("desde") Instant desde,
                                 @Param("hasta") Instant hasta,
                                 @Param("gran") String granularidad);

    @Modifying
    @Transactional
    @Query("DELETE FROM TempestRegistro t WHERE t.timestamp < :before")
    void deleteOlderThan(Instant before);
}
