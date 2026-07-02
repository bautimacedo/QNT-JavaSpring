package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.ReporteActividadResponse;
import com.gestion.qnt.model.ReporteActividad;
import com.gestion.qnt.model.TipoReporte;
import com.gestion.qnt.service.ReporteActividadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/reportes-actividad")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteActividadRestController {

    private final ReporteActividadService service;

    public ReporteActividadRestController(ReporteActividadService service) {
        this.service = service;
    }

    /** Lista la metadata de los reportes (sin el PDF). Filtro opcional por tipo. */
    @GetMapping
    public ResponseEntity<List<ReporteActividadResponse>> list(
            @RequestParam(required = false) TipoReporte tipo) {
        return ResponseEntity.ok(service.listar(tipo).stream()
                .map(ReporteActividadResponse::from).toList());
    }

    /** Descarga el PDF de un reporte. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        ReporteActividad r = service.obtener(id);
        String nombre = service.nombreArchivo(r).replaceAll("[\\r\\n\"\\\\]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(r.getContenido());
    }

    /** Genera (o regenera) el reporte del período que contiene {@code fecha}. Para backfill/manual. */
    @PostMapping("/generar")
    public ResponseEntity<?> generar(
            @RequestParam TipoReporte tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate[] p = tipo == TipoReporte.MENSUAL ? service.mesDe(fecha) : service.semanaDe(fecha);
        ReporteActividad r = service.generarYGuardar(tipo, p[0], p[1]);
        return ResponseEntity.ok(ReporteActividadResponse.from(r));
    }
}
