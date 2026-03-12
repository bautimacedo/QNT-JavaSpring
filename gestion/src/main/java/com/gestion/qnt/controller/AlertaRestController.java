package com.gestion.qnt.controller;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAlertaBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/alertas")
public class AlertaRestController {

    @Autowired
    private IAlertaBusiness alertaBusiness;

    /** GET /api/qnt/v1/alertas/activas — lista alertas no resueltas */
    @GetMapping("/activas")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Alerta>> listActivas() {
        try {
            return ResponseEntity.ok(alertaBusiness.listActivas());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** PUT /api/qnt/v1/alertas/{id}/resolver — marca una alerta como resuelta */
    @PutMapping("/{id}/resolver")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Alerta> resolver(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertaBusiness.resolver(id));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** POST /api/qnt/v1/alertas/generar — fuerza la generación manual (solo ADMIN) */
    @PostMapping("/generar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generarManual() {
        try {
            alertaBusiness.generarAlertas();
            return ResponseEntity.ok().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
