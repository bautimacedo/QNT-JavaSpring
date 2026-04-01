package com.gestion.qnt.repository;

import com.gestion.qnt.model.VueloLog;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VueloLogRepository extends JpaRepository<VueloLog, Long> {

    @Query("""
            SELECT v FROM VueloLog v
            WHERE (:dron   IS NULL OR v.nombreDron = :dron)
              AND (:site   IS NULL OR v.site       = :site)
              AND (:evento IS NULL OR v.evento     = :evento)
              AND (:desde  IS NULL OR v.timestampFlytbase >= :desde)
              AND (:hasta  IS NULL OR v.timestampFlytbase <= :hasta)
            ORDER BY v.timestampFlytbase DESC
            """)
    List<VueloLog> findFiltered(
            @Param("dron")   String dron,
            @Param("site")   String site,
            @Param("evento") TipoEventoVuelo evento,
            @Param("desde")  Instant desde,
            @Param("hasta")  Instant hasta);

    /** Distinct drones que tienen registros. */
    @Query("SELECT DISTINCT v.nombreDron FROM VueloLog v WHERE v.nombreDron IS NOT NULL ORDER BY v.nombreDron")
    List<String> findDistinctDrones();

    /** Distinct sites que tienen registros. */
    @Query("SELECT DISTINCT v.site FROM VueloLog v WHERE v.site IS NOT NULL ORDER BY v.site")
    List<String> findDistinctSites();
}
