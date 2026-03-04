package com.gestion.qnt.controller;

import com.gestion.qnt.model.business.interfaces.IAntenaRtkBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.AntenaRtk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qnt/v1/antenas-rtk")
public class AntenaRtkRestController {

    @Autowired
    private IAntenaRtkBusiness antenaRtkBusiness;

    @GetMapping("")
    public ResponseEntity<List<AntenaRtk>> list() {
        try {
            return new ResponseEntity<>(antenaRtkBusiness.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AntenaRtk> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(antenaRtkBusiness.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    public ResponseEntity<AntenaRtk> add(@RequestBody AntenaRtk antenaRtk) {
        try {
            return new ResponseEntity<>(antenaRtkBusiness.add(antenaRtk), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    public ResponseEntity<AntenaRtk> update(@RequestBody AntenaRtk antenaRtk) {
        try {
            return new ResponseEntity<>(antenaRtkBusiness.update(antenaRtk), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            antenaRtkBusiness.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}