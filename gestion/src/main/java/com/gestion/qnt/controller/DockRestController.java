package com.gestion.qnt.controller;


import com.gestion.qnt.model.business.interfaces.IDockBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.Dock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/docks")
public class DockRestController {

    @Autowired
    private IDockBusiness dockBusiness;

    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Dock>> list() {
        try {
            return new ResponseEntity<>(dockBusiness.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Dock> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(dockBusiness.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Dock> add(@RequestBody Dock dock) {
        try {
            return new ResponseEntity<>(dockBusiness.add(dock), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Dock> update(@RequestBody Dock dock) {
        try {
            return new ResponseEntity<>(dockBusiness.update(dock), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            dockBusiness.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}