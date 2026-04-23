package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.VueloLog;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import com.gestion.qnt.repository.VueloLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoints de solo lectura para el registro de vuelos FlytBase.
 * La escritura la realiza n8n directamente en la base de datos.
 *
 * GET /vuelos-log                      → lista filtrable
 * GET /vuelos-log/stats                → resumen agregado
 * GET /vuelos-log/drones               → drones distintos con registros
 * GET /vuelos-log/sites                → sites distintos con registros
 */
@RestController
@RequestMapping(ApiConstants.URL_BASE + "/vuelos-log")
public class VueloLogRestController {

    private static final Logger log = LoggerFactory.getLogger(VueloLogRestController.class);

    @Autowired
    private VueloLogRepository repository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VueloLog>> list(
            @RequestParam(required = false) String dron,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) TipoEventoVuelo evento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        try {
            String eventoStr = evento != null ? evento.name() : null;
            String desdeStr  = desde  != null ? desde.toString()  : null;
            String hastaStr  = hasta  != null ? hasta.toString()  : null;
            List<VueloLog> regs = repository.findFiltered(dron, site, eventoStr, desdeStr, hastaStr)
                    .stream().filter(r -> r.getEvento() != TipoEventoVuelo.DESPEGUE
                                      && r.getEvento() != TipoEventoVuelo.ATERRIZAJE)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(fillMissingDrones(regs));
        } catch (Exception e) {
            log.error("Error en GET /vuelos-log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats(
            @RequestParam(required = false) String dron,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        try {
            String desdeStr = desde != null ? desde.toString() : null;
            String hastaStr = hasta != null ? hasta.toString() : null;
            List<VueloLog> registros = repository.findFiltered(dron, site, null, desdeStr, hastaStr)
                    .stream().filter(v -> v.getEvento() != TipoEventoVuelo.DESPEGUE
                                      && v.getEvento() != TipoEventoVuelo.ATERRIZAJE)
                    .collect(Collectors.toList());

            long totalVuelos    = registros.stream().filter(v -> v.getEvento() == TipoEventoVuelo.VUELO).count();
            long totalFallas    = registros.stream().filter(v ->
                    v.getEvento() == TipoEventoVuelo.FALLA_DESPEGUE ||
                    v.getEvento() == TipoEventoVuelo.DESPEGUE_FALLIDO ||
                    Boolean.TRUE.equals(v.getDespegueFallido())).count();
            long totalMalTiempo = registros.stream().filter(v -> v.getEvento() == TipoEventoVuelo.MAL_TIEMPO).count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRegistros", registros.size());
            stats.put("totalVuelos",    totalVuelos);
            stats.put("totalFallas",    totalFallas);
            stats.put("totalMalTiempo", totalMalTiempo);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error en GET /vuelos-log/stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/drones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> drones() {
        try {
            return ResponseEntity.ok(repository.findDistinctDrones());
        } catch (Exception e) {
            log.error("Error en GET /vuelos-log/drones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * FlightHub (CAM) emite ATERRIZAJE sin nombreDron. Para esos registros, buscamos
     * el drone más reciente conocido del mismo site y lo propagamos.
     */
    private List<VueloLog> fillMissingDrones(List<VueloLog> registros) {
        // Construir mapa site→drone recorriendo de más antiguo a más reciente
        Map<String, String> dronPorSite = new HashMap<>();
        for (int i = registros.size() - 1; i >= 0; i--) {
            VueloLog r = registros.get(i);
            if (r.getNombreDron() != null && r.getSite() != null) {
                dronPorSite.put(r.getSite(), r.getNombreDron());
            }
        }
        // Rellenar nulls
        for (VueloLog r : registros) {
            if (r.getNombreDron() == null && r.getSite() != null) {
                String fallback = dronPorSite.get(r.getSite());
                if (fallback != null) r.setNombreDron(fallback);
            }
        }
        return registros;
    }

    @GetMapping("/sites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> sites() {
        try {
            return ResponseEntity.ok(repository.findDistinctSites());
        } catch (Exception e) {
            log.error("Error en GET /vuelos-log/sites", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
