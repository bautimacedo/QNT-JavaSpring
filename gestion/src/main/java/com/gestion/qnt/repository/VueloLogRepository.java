package com.gestion.qnt.repository;

import com.gestion.qnt.model.VueloLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VueloLogRepository extends JpaRepository<VueloLog, Long> {

    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE (:dron   IS NULL OR nombre_dron        = :dron)
              AND (:site   IS NULL OR site                = :site)
              AND (:evento IS NULL OR evento              = :evento)
              AND (:desde  IS NULL OR timestamp_flytbase >= CAST(:desde AS timestamptz))
              AND (:hasta  IS NULL OR timestamp_flytbase <= CAST(:hasta AS timestamptz))
            ORDER BY timestamp_flytbase DESC
            """, nativeQuery = true)
    List<VueloLog> findFiltered(
            @Param("dron")   String dron,
            @Param("site")   String site,
            @Param("evento") String evento,
            @Param("desde")  String desde,
            @Param("hasta")  String hasta);

    /** Distinct drones que tienen registros. */
    @Query("SELECT DISTINCT v.nombreDron FROM VueloLog v WHERE v.nombreDron IS NOT NULL ORDER BY v.nombreDron")
    List<String> findDistinctDrones();

    /** Distinct sites que tienen registros. */
    @Query("SELECT DISTINCT v.site FROM VueloLog v WHERE v.site IS NOT NULL ORDER BY v.site")
    List<String> findDistinctSites();

    /** DESPEGUE más reciente (< 12 h) para un drone específico. */
    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE nombre_dron = :nombreDron
              AND evento = 'DESPEGUE'
              AND (despegue_fallido IS NULL OR despegue_fallido = false)
              AND timestamp_flytbase > NOW() - INTERVAL '12 hours'
            ORDER BY timestamp_flytbase DESC LIMIT 1
            """, nativeQuery = true)
    Optional<VueloLog> findLastDespegueByDron(@Param("nombreDron") String nombreDron);

    /** DESPEGUE más reciente (< 12 h) para un site cuando no se conoce el drone. */
    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE site = :site
              AND evento = 'DESPEGUE'
              AND (despegue_fallido IS NULL OR despegue_fallido = false)
              AND timestamp_flytbase > NOW() - INTERVAL '12 hours'
            ORDER BY timestamp_flytbase DESC LIMIT 1
            """, nativeQuery = true)
    Optional<VueloLog> findLastDespegueBySite(@Param("site") String site);

    /**
     * Retorna true si existe un DESPEGUE real (despegueFallido=false) para el drone
     * que NO tiene un ATERRIZAJE posterior — indica que el drone está actualmente volando.
     */
    @Query("""
            SELECT COUNT(v) > 0 FROM VueloLog v
            WHERE v.nombreDron = :nombreDron
              AND v.evento = com.gestion.qnt.model.enums.TipoEventoVuelo.DESPEGUE
              AND v.despegueFallido = false
              AND NOT EXISTS (
                SELECT 1 FROM VueloLog v2
                WHERE v2.nombreDron = :nombreDron
                  AND v2.evento = com.gestion.qnt.model.enums.TipoEventoVuelo.ATERRIZAJE
                  AND v2.timestampFlytbase > v.timestampFlytbase
              )
            """)
    boolean hayVueloActivo(@Param("nombreDron") String nombreDron);
}
