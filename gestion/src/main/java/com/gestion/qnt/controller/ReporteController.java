package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/reportes")
public class ReporteController {

    private static final Path REPORTS_DIR = Paths.get("tmp_report");

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, String>>> listar() {
        File dir = REPORTS_DIR.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return ResponseEntity.ok(List.of());
        }
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".pdf"));
        if (files == null) return ResponseEntity.ok(List.of());
        List<Map<String, String>> result = Arrays.stream(files)
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .map(f -> Map.of("nombre", f.getName(), "tamanio", String.valueOf(f.length())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/descargar/{nombre}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        // Sanitizar nombre para evitar path traversal
        String sanitized = Paths.get(nombre).getFileName().toString();
        Path filePath = REPORTS_DIR.resolve(sanitized);
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitized + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
