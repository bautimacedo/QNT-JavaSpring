package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.ReporteActividad;
import com.gestion.qnt.model.TipoReporte;

import java.time.Instant;
import java.time.LocalDate;

public record ReporteActividadResponse(
        Long id,
        TipoReporte tipo,
        LocalDate periodoDesde,
        LocalDate periodoHasta,
        String titulo,
        Instant createdAt
) {
    public static ReporteActividadResponse from(ReporteActividad r) {
        return new ReporteActividadResponse(
                r.getId(), r.getTipo(), r.getPeriodoDesde(), r.getPeriodoHasta(),
                r.getTitulo(), r.getCreatedAt());
    }
}
