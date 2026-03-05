package com.gestion.qnt.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qnt/v1/system")
public class SystemStatusHealthCheckRestController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "ACTIVO");
        response.put("service", "QNT - Gestión de Operaciones de Drones");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}