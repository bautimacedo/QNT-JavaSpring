package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateSeguroRequest;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Seguro;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.model.business.interfaces.ISeguroBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/seguros")
public class SeguroRestController {

    private final ISeguroBusiness seguroBusiness;
    private final ICompraBusiness compraBusiness;

    public SeguroRestController(ISeguroBusiness seguroBusiness, ICompraBusiness compraBusiness) {
        this.seguroBusiness = seguroBusiness;
        this.compraBusiness = compraBusiness;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Seguro>> list() {
        try {
            return ResponseEntity.ok(seguroBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Seguro> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(seguroBusiness.load(id));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSeguroRequest request) {
        try {
            Compra compra = null;
            if (request.compraId() != null) {
                compra = compraBusiness.load(request.compraId());
            }

            Seguro seguro = new Seguro();
            seguro.setAseguradora(request.aseguradora());
            seguro.setNumeroPoliza(request.numeroPoliza());
            seguro.setVigenciaDesde(request.vigenciaDesde());
            seguro.setVigenciaHasta(request.vigenciaHasta());
            seguro.setObservaciones(request.observaciones());
            seguro.setCompra(compra);

            Seguro created = seguroBusiness.add(seguro);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Compra no encontrada");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateSeguroRequest request) {
        try {
            Seguro existing = seguroBusiness.load(id);

            existing.setAseguradora(request.aseguradora());
            existing.setNumeroPoliza(request.numeroPoliza());
            existing.setVigenciaDesde(request.vigenciaDesde());
            existing.setVigenciaHasta(request.vigenciaHasta());
            existing.setObservaciones(request.observaciones());
            if (request.compraId() != null) {
                existing.setCompra(compraBusiness.load(request.compraId()));
            } else {
                existing.setCompra(null);
            }

            Seguro updated = seguroBusiness.update(existing);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            seguroBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
