package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.ProgramacionMision;
import com.gestion.qnt.repository.ProgramacionMisionRepository;
import com.gestion.qnt.service.ProgramacionMisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MisionSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(MisionSchedulerJob.class);

    @Autowired
    private ProgramacionMisionService service;

    @Autowired
    private ProgramacionMisionRepository repo;

    // Sin @Transactional aquí: cada llamada al service abre su propia tx (REQUIRES_NEW).
    // Así un fallo en una programación no afecta a las demás ni deja la tx outer en rollback-only.
    @Scheduled(fixedRate = 60000)
    public void ejecutar() {
        List<ProgramacionMision> pendientes =
                repo.findByActivaTrueAndProxEjecucionBefore(LocalDateTime.now().plusSeconds(30));
        if (pendientes.isEmpty()) return;

        log.info("Scheduler: {} programaciones listas para ejecutar", pendientes.size());

        for (ProgramacionMision p : pendientes) {
            LocalDate hoy = LocalDate.now();

            if (p.getFechaFinVigencia() != null && hoy.isAfter(p.getFechaFinVigencia())) {
                try { service.desactivar(p); } catch (Exception e) { log.error("Error desactivando {}: {}", p.getId(), e.getMessage()); }
                log.info("Programación {} desactivada por fin de vigencia", p.getId());
                continue;
            }

            if (p.getFechaInicioVigencia() != null && hoy.isBefore(p.getFechaInicioVigencia())) {
                continue;
            }

            try {
                service.generarYLanzar(p);
            } catch (Exception e) {
                log.error("Error ejecutando programación {}: {}", p.getId(), e.getMessage());
            }

            try {
                service.actualizarProxEjecucion(p);
            } catch (Exception e) {
                log.error("Error actualizando proxEjecucion para {}: {}", p.getId(), e.getMessage());
            }
        }
    }
}
