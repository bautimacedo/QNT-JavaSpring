package com.gestion.qnt.repository;

import com.gestion.qnt.model.LicenciaANAC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LicenciaANACRepository extends JpaRepository<LicenciaANAC, Long> {

    List<LicenciaANAC> findByPilotoId(Long pilotoId);

    /** Licencias activas con CMA vencida (fecha < hoy). */
    @Query("SELECT l FROM LicenciaANAC l JOIN FETCH l.piloto WHERE l.activo = true AND l.fechaVencimientoCma IS NOT NULL AND l.fechaVencimientoCma < :hoy")
    List<LicenciaANAC> findActivasConCmaVencida(@Param("hoy") LocalDate hoy);

    /** IDs de pilotos que tienen al menos una licencia activa. */
    @Query("SELECT DISTINCT l.piloto.id FROM LicenciaANAC l WHERE l.activo = true")
    List<Long> findPilotoIdsConLicenciaActiva();
}
