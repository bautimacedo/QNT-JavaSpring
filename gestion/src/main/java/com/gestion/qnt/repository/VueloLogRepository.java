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
            WHERE (:dron   IS NULL OR nombre_dron = :dron)
              AND (:site   IS NULL OR site         = :site)
              AND (:evento IS NULL OR evento        = :evento)
              AND (:desde  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) >= CAST(:desde AS timestamptz))
              AND (:hasta  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) <= CAST(:hasta AS timestamptz))
            ORDER BY COALESCE(timestamp_flytbase, fecha_registro) DESC
            """, nativeQuery = true)
    List<VueloLog> findFiltered(
            @Param("dron")   String dron,
            @Param("site")   String site,
            @Param("evento") String evento,
            @Param("desde")  String desde,
            @Param("hasta")  String hasta);

    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE (:dron   IS NULL OR nombre_dron = :dron)
              AND (:site   IS NULL OR site         = :site)
              AND (:evento IS NULL OR evento        = :evento)
              AND (:desde  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) >= CAST(:desde AS timestamptz))
              AND (:hasta  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) <= CAST(:hasta AS timestamptz))
            ORDER BY COALESCE(timestamp_flytbase, fecha_registro) DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<VueloLog> findFilteredPaged(
            @Param("dron")   String dron,
            @Param("site")   String site,
            @Param("evento") String evento,
            @Param("desde")  String desde,
            @Param("hasta")  String hasta,
            @Param("size")   int size,
            @Param("offset") long offset);

    @Query(value = """
            SELECT COUNT(*) FROM vuelos_log
            WHERE (:dron   IS NULL OR nombre_dron = :dron)
              AND (:site   IS NULL OR site         = :site)
              AND (:evento IS NULL OR evento        = :evento)
              AND (:desde  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) >= CAST(:desde AS timestamptz))
              AND (:hasta  IS NULL OR COALESCE(timestamp_flytbase, fecha_registro) <= CAST(:hasta AS timestamptz))
            """, nativeQuery = true)
    long countFiltered(
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

    /** DESPEGUE más reciente (< 12 h) para un drone específico. Usa fecha_registro como fallback si timestamp_flytbase es NULL. */
    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE nombre_dron = :nombreDron
              AND evento = 'DESPEGUE'
              AND (despegue_fallido IS NULL OR despegue_fallido = false)
              AND COALESCE(timestamp_flytbase, fecha_registro) > NOW() - INTERVAL '12 hours'
            ORDER BY COALESCE(timestamp_flytbase, fecha_registro) DESC LIMIT 1
            """, nativeQuery = true)
    Optional<VueloLog> findLastDespegueByDron(@Param("nombreDron") String nombreDron);

    /** DESPEGUE más reciente (< 12 h) para un site cuando no se conoce el drone. */
    @Query(value = """
            SELECT * FROM vuelos_log
            WHERE site = :site
              AND evento = 'DESPEGUE'
              AND (despegue_fallido IS NULL OR despegue_fallido = false)
              AND COALESCE(timestamp_flytbase, fecha_registro) > NOW() - INTERVAL '12 hours'
            ORDER BY COALESCE(timestamp_flytbase, fecha_registro) DESC LIMIT 1
            """, nativeQuery = true)
    Optional<VueloLog> findLastDespegueBySite(@Param("site") String site);

    /**
     * Retorna true si existe un DESPEGUE real (despegueFallido=false) para el drone
     * que NO tiene un ATERRIZAJE posterior — indica que el drone está actualmente volando.
     * Usa COALESCE(timestamp_flytbase, fecha_registro) para soportar registros sin timestamp FlytBase.
     */
    @Query(value = """
            SELECT COUNT(*) > 0 FROM vuelos_log v
            WHERE v.nombre_dron = :nombreDron
              AND v.evento = 'DESPEGUE'
              AND (v.despegue_fallido IS NULL OR v.despegue_fallido = false)
              AND NOT EXISTS (
                SELECT 1 FROM vuelos_log v2
                WHERE v2.nombre_dron = :nombreDron
                  AND v2.evento = 'ATERRIZAJE'
                  AND COALESCE(v2.timestamp_flytbase, v2.fecha_registro)
                    > COALESCE(v.timestamp_flytbase, v.fecha_registro)
              )
            """, nativeQuery = true)
    boolean hayVueloActivo(@Param("nombreDron") String nombreDron);

    /** true si existe un DESPEGUE para el drone registrado después de :desde. */
    @Query(value = "SELECT COUNT(*) > 0 FROM vuelos_log WHERE nombre_dron = :dron AND evento = 'DESPEGUE' AND fecha_registro > :desde",
           nativeQuery = true)
    boolean existsDespegueAfter(@Param("dron") String dron, @Param("desde") java.time.LocalDateTime desde);
}
