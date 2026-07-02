package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.ReporteFalla;
import com.gestion.qnt.model.VueloLog;
import com.gestion.qnt.model.enums.TipoEventoVuelo;
import com.gestion.qnt.repository.ReporteFallaRepository;
import com.gestion.qnt.repository.VueloLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/reportes")
public class ReporteController {

    @Autowired
    private ReporteFallaRepository reporteFallaRepository;

    @Autowired
    private VueloLogRepository vueloLogRepository;

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    // Archivos de ejemplo embebidos en el JAR
    private static final List<String> EJEMPLO_FILES = List.of(
            "Informe_Termografico_20260327_140630.pdf",
            "Mantenimiento_Predictivo-informe_termografico_CAm-194-72.pdf"
    );

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String nombre : EJEMPLO_FILES) {
            result.add(Map.of("nombre", nombre, "tamanio", 0L));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/descargar/{nombre}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        String sanitized = Paths.get(nombre).getFileName().toString();
        if (!sanitized.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().build();
        }
        Resource resource = new ClassPathResource("static/reports/" + sanitized);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String safeStaticFilename = sanitized.replaceAll("[\\r\\n\"\\\\]", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeStaticFilename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    // ── Reportes de fallas ────────────────────────────────────────────────────

    @GetMapping("/fallas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listarFallas() {
        List<Map<String, Object>> result = reporteFallaRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",            r.getId());
                    m.put("titulo",        r.getTitulo());
                    m.put("fecha",         r.getFecha().toString());
                    m.put("archivoNombre", r.getArchivoNombre());
                    m.put("fechaSubida",   r.getFechaSubida().toString());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/fallas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> subirFalla(
            @RequestParam("titulo") String titulo,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("archivo") MultipartFile archivo) throws java.io.IOException {

        String fn = archivo.getOriginalFilename();
        if (archivo.isEmpty() || fn == null || !fn.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos PDF"));
        }
        byte[] bytes = archivo.getBytes();
        if (bytes.length < 4 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no es un PDF válido"));
        }

        // Basename por String (no Paths.get): el nombre puede tener acentos/ñ y el charset del
        // filesystem del contenedor los rechaza (InvalidPathException → 500 al guardar).
        String nombreArchivo = fn.substring(Math.max(fn.lastIndexOf('/'), fn.lastIndexOf('\\')) + 1);

        ReporteFalla r = new ReporteFalla();
        r.setTitulo(titulo);
        r.setFecha(fecha);
        r.setArchivoNombre(nombreArchivo);
        r.setContenido(archivo.getBytes());
        r.setFechaSubida(Instant.now());
        reporteFallaRepository.save(r);

        return ResponseEntity.status(201).body(Map.of(
                "id",     r.getId(),
                "titulo", r.getTitulo(),
                "fecha",  r.getFecha().toString()));
    }

    @GetMapping("/fallas/{id}/descargar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarFalla(@PathVariable Long id) {
        return reporteFallaRepository.findById(id)
                .map(r -> {
                    String safeFilename = r.getArchivoNombre().replaceAll("[\\r\\n\"\\\\]", "_");
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + safeFilename + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(r.getContenido());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/fallas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarFalla(@PathVariable Long id) {
        if (!reporteFallaRepository.existsById(id)) return ResponseEntity.notFound().build();
        reporteFallaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Reportes diarios ─────────────────────────────────────────────────────

    /**
     * Retorna un resumen por día de la actividad de vuelo.
     * desde/hasta: fechas en formato YYYY-MM-DD (Argentina).
     */
    @GetMapping("/diarios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listarDiarios(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        String desdeTs = desde != null ? desde + "T00:00:00-03:00" : null;
        String hastaTs = hasta != null ? hasta + "T23:59:59-03:00" : null;

        List<VueloLog> registros = vueloLogRepository.findFiltered(null, null, null, desdeTs, hastaTs);

        // Para sites != EFO, ATERRIZAJE/DESPEGUE cuenta como VUELO
        for (VueloLog v : registros) {
            if (!"EFO".equalsIgnoreCase(v.getSite())
                    && (v.getEvento() == TipoEventoVuelo.ATERRIZAJE
                        || v.getEvento() == TipoEventoVuelo.DESPEGUE)) {
                v.setEvento(TipoEventoVuelo.VUELO);
            }
        }

        // Agrupar por fecha local (Argentina)
        Map<LocalDate, List<VueloLog>> porFecha = new TreeMap<>(Comparator.reverseOrder());
        for (VueloLog v : registros) {
            Instant ts = v.getTimestampFlytbase() != null
                    ? v.getTimestampFlytbase()
                    : (v.getFechaRegistro() != null ? v.getFechaRegistro().atZone(ARG).toInstant() : null);
            if (ts == null) continue;
            LocalDate fecha = ts.atZone(ARG).toLocalDate();
            porFecha.computeIfAbsent(fecha, k -> new ArrayList<>()).add(v);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        porFecha.forEach((fecha, logs) -> {
            long vuelos = logs.stream().filter(v -> v.getEvento() == TipoEventoVuelo.VUELO).count();
            long fallas = logs.stream().filter(v ->
                    v.getEvento() == TipoEventoVuelo.FALLA_DESPEGUE ||
                    v.getEvento() == TipoEventoVuelo.DESPEGUE_FALLIDO ||
                    Boolean.TRUE.equals(v.getDespegueFallido())).count();
            long cortos = logs.stream().filter(v ->
                    v.getEvento() == TipoEventoVuelo.VUELO
                    && v.getDuracionMinutos() != null && v.getDuracionMinutos() < 3).count();
            List<String> sites = logs.stream()
                    .map(VueloLog::getSite)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fecha",             fecha.toString());
            m.put("totalVuelos",       vuelos);
            m.put("totalFallas",       fallas);
            m.put("totalVuelosCortos", cortos);
            m.put("sites",             sites);
            result.add(m);
        });

        return ResponseEntity.ok(result);
    }
}
