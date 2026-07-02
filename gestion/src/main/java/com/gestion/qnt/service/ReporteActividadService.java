package com.gestion.qnt.service;

import com.gestion.qnt.controller.dto.ResumenHorasResponse;
import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.ReporteActividad;
import com.gestion.qnt.model.TipoReporte;
import com.gestion.qnt.repository.ReporteActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteActividadService {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Locale ES = Locale.forLanguageTag("es");
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES);

    private final ReporteActividadRepository repo;
    private final RegistroHoraService registroHoraService;
    private final ReporteActividadPdfService pdfService;

    public ReporteActividadService(ReporteActividadRepository repo,
                                   RegistroHoraService registroHoraService,
                                   ReporteActividadPdfService pdfService) {
        this.repo = repo;
        this.registroHoraService = registroHoraService;
        this.pdfService = pdfService;
    }

    /** Genera el PDF del período y lo persiste (upsert por tipo+período). Devuelve la entidad. */
    @Transactional
    public ReporteActividad generarYGuardar(TipoReporte tipo, LocalDate desde, LocalDate hasta) {
        List<RegistroHora> registros = registroHoraService.listar(desde, hasta);
        List<ResumenHorasResponse> resumen = registroHoraService.resumen(desde, hasta);
        String titulo = titulo(tipo, desde, hasta);
        byte[] pdf = pdfService.generar(titulo, desde, hasta, registros, resumen);

        ReporteActividad r = repo.findByTipoAndPeriodoDesdeAndPeriodoHasta(tipo, desde, hasta)
                .orElseGet(ReporteActividad::new);
        r.setTipo(tipo);
        r.setPeriodoDesde(desde);
        r.setPeriodoHasta(hasta);
        r.setTitulo(titulo);
        r.setContenido(pdf);
        r.setCreatedAt(Instant.now());
        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public List<ReporteActividad> listar(TipoReporte tipo) {
        return tipo != null
                ? repo.findByTipoOrderByPeriodoDesdeDesc(tipo)
                : repo.findAllByOrderByPeriodoDesdeDesc();
    }

    @Transactional(readOnly = true)
    public ReporteActividad obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado: " + id));
    }

    /** Nombre de archivo sugerido para descarga/adjunto. */
    public String nombreArchivo(ReporteActividad r) {
        return "reporte-" + r.getTipo().name().toLowerCase() + "-"
                + r.getPeriodoDesde() + "_" + r.getPeriodoHasta() + ".pdf";
    }

    // ── Cálculo de períodos ──────────────────────────────────────────────

    /** Semana calendario anterior (lunes a domingo) respecto de {@code hoy}. */
    public LocalDate[] semanaPasada(LocalDate hoy) {
        LocalDate lunesPasado = hoy.with(DayOfWeek.MONDAY).minusWeeks(1);
        return new LocalDate[]{lunesPasado, lunesPasado.plusDays(6)};
    }

    /** Mes calendario anterior respecto de {@code hoy}. */
    public LocalDate[] mesPasado(LocalDate hoy) {
        YearMonth ym = YearMonth.from(hoy).minusMonths(1);
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    /** Semana (lunes a domingo) que contiene {@code fecha}. Para generación manual/backfill. */
    public LocalDate[] semanaDe(LocalDate fecha) {
        LocalDate lunes = fecha.with(DayOfWeek.MONDAY);
        return new LocalDate[]{lunes, lunes.plusDays(6)};
    }

    /** Mes calendario que contiene {@code fecha}. Para generación manual/backfill. */
    public LocalDate[] mesDe(LocalDate fecha) {
        YearMonth ym = YearMonth.from(fecha);
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    public static ZoneId zona() { return ARG; }

    private String titulo(TipoReporte tipo, LocalDate desde, LocalDate hasta) {
        if (tipo == TipoReporte.MENSUAL) {
            String mes = desde.format(F_MES);
            return "Reporte Mensual de Actividades — " + mes.substring(0, 1).toUpperCase() + mes.substring(1);
        }
        return "Reporte Semanal de Actividades — " + desde.format(F_FECHA) + " al " + hasta.format(F_FECHA);
    }
}
