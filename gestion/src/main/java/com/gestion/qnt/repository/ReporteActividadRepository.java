package com.gestion.qnt.repository;

import com.gestion.qnt.model.ReporteActividad;
import com.gestion.qnt.model.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReporteActividadRepository extends JpaRepository<ReporteActividad, Long> {

    List<ReporteActividad> findAllByOrderByPeriodoDesdeDesc();

    List<ReporteActividad> findByTipoOrderByPeriodoDesdeDesc(TipoReporte tipo);

    Optional<ReporteActividad> findByTipoAndPeriodoDesdeAndPeriodoHasta(
            TipoReporte tipo, LocalDate periodoDesde, LocalDate periodoHasta);
}
