package com.gestion.qnt.controller;

import com.gestion.qnt.model.AntenaStarlink;
import com.gestion.qnt.model.business.interfaces.IAntenaStarlinkBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/antenas-starlink")
public class AntenaStarlinkRestController {

    @Autowired
    private IAntenaStarlinkBusiness antenaStarlinkBusiness;

    @GetMapping("")
    public ResponseEntity<List<AntenaStarlink>> list() {
        try {
            return new ResponseEntity<>(antenaStarlinkBusiness.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AntenaStarlink> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(antenaStarlinkBusiness.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    public ResponseEntity<AntenaStarlink> add(@RequestBody AntenaStarlink antenaStarlink) {
        try {
            return new ResponseEntity<>(antenaStarlinkBusiness.add(antenaStarlink), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    public ResponseEntity<AntenaStarlink> update(@RequestBody AntenaStarlink antenaStarlink) {
        try {
            return new ResponseEntity<>(antenaStarlinkBusiness.update(antenaStarlink), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            antenaStarlinkBusiness.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}