package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/reportes")
public class ReporteController {

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
            Resource r = new ClassPathResource("static/reports/" + nombre);
            if (r.exists()) {
                try {
                    result.add(Map.of("nombre", nombre, "tamanio", r.contentLength()));
                } catch (IOException e) {
                    result.add(Map.of("nombre", nombre, "tamanio", 0L));
                }
            }
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
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitized + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
