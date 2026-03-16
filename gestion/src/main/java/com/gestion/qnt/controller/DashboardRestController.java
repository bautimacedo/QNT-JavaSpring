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
        long dronesTotal          = dronRepository.count();
        long dronesOperativos     = dronRepository.findByEstado(Estado.STOCK_ACTIVO).size();
        long dronesEnMant         = dronRepository.findByEstado(Estado.EN_MANTENIMIENTO).size();
        long dronesEnDesuso       = dronRepository.findByEstado(Estado.EN_DESUSO).size();

        long misionesTotal        = misionRepository.count();
        long planificadas         = misionRepository.findByEstado(EstadoMision.PLANIFICADA).size();
        long enCurso              = misionRepository.findByEstado(EstadoMision.EN_CURSO).size();
        long completadas          = misionRepository.findByEstado(EstadoMision.COMPLETADA).size();
        long canceladas           = misionRepository.findByEstado(EstadoMision.CANCELADA).size();

        long alertasActivas       = alertaRepository.countByResueltaFalse();

        long batCiclos            = bateriaRepository.findByCiclosCargaGreaterThan(CICLOS_MAX).size();
        long dronsTempAlta        = dronRepository.findByBateriaTempCGreaterThan(TEMP_BAT_MAX).size();

        return ResponseEntity.ok(new DashboardStatsResponse(
                dronesTotal, dronesOperativos, dronesEnMant, dronesEnDesuso,
                misionesTotal, planificadas, enCurso, completadas, canceladas,
                alertasActivas,
                batCiclos, dronsTempAlta
        ));
    }
}
