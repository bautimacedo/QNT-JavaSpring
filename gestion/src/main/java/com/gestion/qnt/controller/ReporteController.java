package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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

    private static final String REPORTS_CLASSPATH = "classpath:static/reports/";

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(REPORTS_CLASSPATH + "*.pdf");
            List<Map<String, Object>> result = new ArrayList<>();
            for (Resource r : resources) {
                result.add(Map.of(
                        "nombre", r.getFilename(),
                        "tamanio", r.contentLength()
                ));
            }
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/descargar/{nombre}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        // Sanitizar para evitar path traversal
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
