package com.gestion.qnt.scheduler;

import com.gestion.qnt.model.ReporteActividad;
import com.gestion.qnt.model.TipoReporte;
import com.gestion.qnt.service.EmailService;
import com.gestion.qnt.service.ReporteActividadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Genera y envía por email los reportes de actividades semanal (lunes) y mensual (día 1). */
@Component
public class ReporteActividadesJob {

    private static final Logger log = LoggerFactory.getLogger(ReporteActividadesJob.class);
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReporteActividadService reporteService;
    private final EmailService emailService;

    public ReporteActividadesJob(ReporteActividadService reporteService, EmailService emailService) {
        this.reporteService = reporteService;
        this.emailService = emailService;
    }

    /** Lunes 07:00 ART — actividades de la semana pasada (lunes a domingo). */
    @Scheduled(cron = "0 0 7 * * MON", zone = "America/Argentina/Buenos_Aires")
    public void semanal() {
        LocalDate[] p = reporteService.semanaPasada(LocalDate.now(ReporteActividadService.zona()));
        generarYEnviar(TipoReporte.SEMANAL, p[0], p[1], "[QNT] Reporte Semanal de Actividades");
    }

    /** Día 1 de cada mes 07:00 ART — actividades del mes calendario anterior. */
    @Scheduled(cron = "0 0 7 1 * *", zone = "America/Argentina/Buenos_Aires")
    public void mensual() {
        LocalDate[] p = reporteService.mesPasado(LocalDate.now(ReporteActividadService.zona()));
        generarYEnviar(TipoReporte.MENSUAL, p[0], p[1], "[QNT] Reporte Mensual de Actividades");
    }

    private void generarYEnviar(TipoReporte tipo, LocalDate desde, LocalDate hasta, String asunto) {
        try {
            ReporteActividad r = reporteService.generarYGuardar(tipo, desde, hasta);
            String periodo = "Período: " + desde.format(F_FECHA) + " al " + hasta.format(F_FECHA);
            emailService.sendReporteActividades(asunto, r.getTitulo(), periodo,
                    r.getContenido(), reporteService.nombreArchivo(r));
            log.info("ReporteActividadesJob[{}]: generado y enviado ({} al {})", tipo, desde, hasta);
        } catch (Exception e) {
            log.error("ReporteActividadesJob[{}]: error generando/enviando el reporte", tipo, e);
        }
    }
}
