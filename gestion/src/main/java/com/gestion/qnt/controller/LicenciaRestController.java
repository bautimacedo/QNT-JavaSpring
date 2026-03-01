package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateLicenciaRequest;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Licencia;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.model.business.interfaces.ILicenciaBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/licencias")
public class LicenciaRestController {

    private final ILicenciaBusiness licenciaBusiness;
    private final ICompraBusiness compraBusiness;

    public LicenciaRestController(ILicenciaBusiness licenciaBusiness, ICompraBusiness compraBusiness) {
        this.licenciaBusiness = licenciaBusiness;
        this.compraBusiness = compraBusiness;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Licencia>> list() {
        try {
            return ResponseEntity.ok(licenciaBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Licencia> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(licenciaBusiness.load(id));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateLicenciaRequest request) {
        try {
            Compra compra = null;
            if (request.compraId() != null) {
                compra = compraBusiness.load(request.compraId());
            }

            Licencia licencia = new Licencia();
            licencia.setNombre(request.nombre());
            licencia.setNumLicencia(request.numLicencia());
            licencia.setCompra(compra);
            licencia.setFechaCompra(request.fechaCompra());
            licencia.setCaducidad(request.caducidad());
            licencia.setVersion(request.version());
            licencia.setActivo(request.activo() != null ? request.activo() : true);

            Licencia created = licenciaBusiness.add(licencia);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Compra no encontrada");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateLicenciaRequest request) {
        try {
            Licencia existing = licenciaBusiness.load(id);

            existing.setNombre(request.nombre());
            existing.setNumLicencia(request.numLicencia());
            existing.setFechaCompra(request.fechaCompra());
            existing.setCaducidad(request.caducidad());
            existing.setVersion(request.version());
            if (request.activo() != null) {
                existing.setActivo(request.activo());
            }
            if (request.compraId() != null) {
                existing.setCompra(compraBusiness.load(request.compraId()));
            } else {
                existing.setCompra(null);
            }

            Licencia updated = licenciaBusiness.update(existing);
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
            licenciaBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
