package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.ReporteFalla;
import com.gestion.qnt.repository.ReporteFallaRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/reportes")
public class ReporteController {

    @Autowired
    private ReporteFallaRepository reporteFallaRepository;

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
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        String sanitized = Paths.get(nombre).getFileName().toString();
        if (!sanitized.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().build();
        }
        Resource resource = new ClassPathResource("static/reports/" + sanitized);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitized + "\"")
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
            @RequestParam("archivo") MultipartFile archivo) throws Exception {

        if (archivo.isEmpty() || !archivo.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos PDF"));
        }

        ReporteFalla r = new ReporteFalla();
        r.setTitulo(titulo);
        r.setFecha(fecha);
        r.setArchivoNombre(Paths.get(archivo.getOriginalFilename()).getFileName().toString());
        r.setContenido(archivo.getBytes());
        r.setFechaSubida(Instant.now());
        reporteFallaRepository.save(r);

        return ResponseEntity.status(201).body(Map.of(
                "id",     r.getId(),
                "titulo", r.getTitulo(),
                "fecha",  r.getFecha().toString()));
    }

    @GetMapping("/fallas/{id}/descargar")
    public ResponseEntity<byte[]> descargarFalla(@PathVariable Long id) {
        return reporteFallaRepository.findById(id)
                .map(r -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + r.getArchivoNombre() + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(r.getContenido()))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/fallas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarFalla(@PathVariable Long id) {
        if (!reporteFallaRepository.existsById(id)) return ResponseEntity.notFound().build();
        reporteFallaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
