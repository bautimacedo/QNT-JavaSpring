package com.gestion.qnt.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.Empresa;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IEmpresaBusiness;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/empresas")
public class EmpresaRestController {
	
	@Autowired
    private IEmpresaBusiness empresaService;
	
	@GetMapping("")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Empresa>> list() {
        try {
            return new ResponseEntity<>(empresaService.list(), HttpStatus.OK);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Empresa> load(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(empresaService.load(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Empresa> add(@RequestBody Empresa empresa) {
        try {
            return new ResponseEntity<>(empresaService.add(empresa), HttpStatus.CREATED);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Empresa> update(@RequestBody Empresa empresa) {
        try {
            return new ResponseEntity<>(empresaService.update(empresa), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Empresa> delete(@PathVariable("id") Long id) {
        try {
            return new ResponseEntity<>(empresaService.delete(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint específico para agregar un Site a una Empresa existente
    @PostMapping("/{id}/sites")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Empresa> addSite(@PathVariable("id") Long id, @RequestBody Site site) {
        try {
            return new ResponseEntity<>(empresaService.addSite(site, id), HttpStatus.CREATED);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (BusinessException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
	
	

}
