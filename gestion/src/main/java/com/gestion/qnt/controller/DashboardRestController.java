package com.gestion.qnt.controller;

import com.gestion.qnt.controller.dto.DashboardStatsResponse;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.repository.AlertaRepository;
import com.gestion.qnt.repository.BateriaRepository;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.MisionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/dashboard")
public class DashboardRestController {

    private static final BigDecimal TEMP_BAT_MAX  = new BigDecimal("50");
    private static final int        CICLOS_MAX    = 250;

    private final DronRepository    dronRepository;
    private final MisionRepository  misionRepository;
    private final AlertaRepository  alertaRepository;
    private final BateriaRepository bateriaRepository;

    public DashboardRestController(DronRepository dronRepository,
                                   MisionRepository misionRepository,
                                   AlertaRepository alertaRepository,
                                   BateriaRepository bateriaRepository) {
        this.dronRepository    = dronRepository;
        this.misionRepository  = misionRepository;
        this.alertaRepository  = alertaRepository;
        this.bateriaRepository = bateriaRepository;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardStatsResponse> stats() {
        // 2 queries de agregación reemplazan las 8 originales (findByEstado × 4 por entidad)
        List<Object[]> dronCounts   = dronRepository.countGroupByEstado();
        List<Object[]> misionCounts = misionRepository.countGroupByEstado();

        long dronesTotal      = dronRepository.count();
        long dronesOperativos = extractCount(dronCounts, Estado.STOCK_ACTIVO);
        long dronesEnMant     = extractCount(dronCounts, Estado.EN_MANTENIMIENTO);
        long dronesEnDesuso   = extractCount(dronCounts, Estado.EN_DESUSO);

        long misionesTotal = misionRepository.count();
        long planificadas  = extractCount(misionCounts, EstadoMision.PLANIFICADA);
        long enCurso       = extractCount(misionCounts, EstadoMision.EN_CURSO);
        long completadas   = extractCount(misionCounts, EstadoMision.COMPLETADA);
        long canceladas    = extractCount(misionCounts, EstadoMision.CANCELADA);

        long alertasActivas = alertaRepository.countByResueltaFalse();
        long batCiclos      = bateriaRepository.countByCiclosCargaGreaterThan(CICLOS_MAX);
        long dronsTempAlta  = dronRepository.countByBateriaTempCGreaterThan(TEMP_BAT_MAX);

        return ResponseEntity.ok(new DashboardStatsResponse(
                dronesTotal, dronesOperativos, dronesEnMant, dronesEnDesuso,
                misionesTotal, planificadas, enCurso, completadas, canceladas,
                alertasActivas,
                batCiclos, dronsTempAlta
        ));
    }

    private long extractCount(List<Object[]> rows, Enum<?> estado) {
        for (Object[] row : rows) {
            if (estado.equals(row[0])) return ((Number) row[1]).longValue();
        }
        return 0L;
    }
}
