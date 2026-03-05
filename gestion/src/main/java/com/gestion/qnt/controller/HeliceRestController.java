package com.gestion.qnt.controller;

import com.gestion.qnt.model.Helice;
import com.gestion.qnt.model.business.interfaces.IHeliceBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/helices")
public class HeliceRestController {

    @Autowired
    private IHeliceBusiness heliceBusiness;

    @GetMapping("")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Helice>> list() {
        try {
            return new ResponseEntity<>(heliceBusiness.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Helice> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(heliceBusiness.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Helice> add(@RequestBody Helice helice) {
        try {
            return new ResponseEntity<>(heliceBusiness.add(helice), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Helice> update(@RequestBody Helice helice) {
        try {
            return new ResponseEntity<>(heliceBusiness.update(helice), HttpStatus.OK);
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
            heliceBusiness.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}