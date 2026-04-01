package com.gestion.qnt.repository;

import com.gestion.qnt.model.VueloLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
