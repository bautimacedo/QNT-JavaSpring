package com.gestion.qnt.controller;

import com.gestion.qnt.model.Dock;
import com.gestion.qnt.model.Dron;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IDronBusiness;
import com.gestion.qnt.repository.DockRepository;
import com.gestion.qnt.repository.DronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qnt/v1/drones")
public class DronRestController {

    @Autowired private IDronBusiness dronBusiness;
    @Autowired private DronRepository dronRepository;
    @Autowired private DockRepository dockRepository;

    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Dron>> list() {
        try {
            return new ResponseEntity<>(dronBusiness.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Dron> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(dronBusiness.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Dron> add(@RequestBody Dron dron) {
        try {
            return new ResponseEntity<>(dronBusiness.add(dron), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Dron> update(@RequestBody Dron dron) {
        try {
            return new ResponseEntity<>(dronBusiness.update(dron), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/dock")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> setDock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Dron dron = dronRepository.findById(id).orElse(null);
        if (dron == null) return ResponseEntity.notFound().build();
        Object dockIdRaw = body.get("dockId");
        if (dockIdRaw == null) {
            dron.setDock(null);
        } else {
            Long dockId = ((Number) dockIdRaw).longValue();
            Dock dock = dockRepository.findById(dockId).orElse(null);
            if (dock == null) return ResponseEntity.badRequest().body(Map.of("error", "Dock no encontrado"));
            dron.setDock(dock);
        }
        dronRepository.save(dron);
        return ResponseEntity.ok(Map.of("dronId", id, "dockId", dockIdRaw != null ? dockIdRaw : "null"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            dronBusiness.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}