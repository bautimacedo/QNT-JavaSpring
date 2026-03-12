package com.gestion.qnt.model.business;

import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.interfaces.IAlertaBusiness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertaScheduler {

    @Autowired
    private IAlertaBusiness alertaBusiness;

    /**
     * Ejecuta la generación de alertas:
     * - Al arrancar la aplicación (initialDelay = 0)
     * - Todos los días a las 06:00 AM
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void generarAlertasDiarias() {
        log.info("Scheduler: generando alertas...");
        try {
            alertaBusiness.generarAlertas();
        } catch (BusinessException e) {
            log.error("Error en scheduler de alertas", e);
        }
    }
}
