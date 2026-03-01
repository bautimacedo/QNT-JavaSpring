package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateCompraRequest;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Proveedor;
import com.gestion.qnt.model.Site;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.model.business.interfaces.IProveedorBusiness;
import com.gestion.qnt.model.business.interfaces.ISiteBusiness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/compras")
public class CompraRestController {

    private final ICompraBusiness compraBusiness;
    private final IProveedorBusiness proveedorBusiness;
    private final ISiteBusiness siteBusiness;

    public CompraRestController(ICompraBusiness compraBusiness,
                                IProveedorBusiness proveedorBusiness,
                                ISiteBusiness siteBusiness) {
        this.compraBusiness = compraBusiness;
        this.proveedorBusiness = proveedorBusiness;
        this.siteBusiness = siteBusiness;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Compra>> list() {
        try {
            return ResponseEntity.ok(compraBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Compra> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(compraBusiness.load(id));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateCompraRequest request) {
        try {
            Proveedor proveedor = proveedorBusiness.load(request.proveedorId());
            Site site = null;
            if (request.siteId() != null) {
                site = siteBusiness.load(request.siteId());
            }

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setFechaCompra(request.fechaCompra());
            compra.setFechaFactura(request.fechaFactura());
            compra.setNumeroFactura(request.numeroFactura());
            compra.setImporte(request.importe());
            compra.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            compra.setTipoCompra(request.tipoCompra());
            compra.setDescripcion(request.descripcion());
            compra.setSite(site);
            compra.setObservaciones(request.observaciones());

            Compra created = compraBusiness.add(compra);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Proveedor o Site no encontrado");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateCompraRequest request) {
        try {
            Compra existing = compraBusiness.load(id);

            Proveedor proveedor = proveedorBusiness.load(request.proveedorId());
            existing.setProveedor(proveedor);
            existing.setFechaCompra(request.fechaCompra());
            existing.setFechaFactura(request.fechaFactura());
            existing.setNumeroFactura(request.numeroFactura());
            existing.setImporte(request.importe());
            existing.setMoneda(request.moneda() != null && !request.moneda().isBlank() ? request.moneda() : "ARS");
            existing.setTipoCompra(request.tipoCompra());
            existing.setDescripcion(request.descripcion());
            if (request.siteId() != null) {
                existing.setSite(siteBusiness.load(request.siteId()));
            } else {
                existing.setSite(null);
            }
            existing.setObservaciones(request.observaciones());

            Compra updated = compraBusiness.update(existing);
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
            compraBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Sube la imagen de la factura para la compra indicada.
     * Requiere multipart/form-data con parte "file".
     */
    @PutMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> uploadImagen(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) {
        try {
            Compra compra = compraBusiness.load(id);
            compra.setImagenFactura(file.getBytes());
            compraBusiness.update(compra);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene la imagen de la factura de la compra.
     * 404 si la compra no existe o no tiene imagen.
     */
    @GetMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<byte[]> getImagen(@PathVariable Long id) {
        try {
            Compra compra = compraBusiness.load(id);
            byte[] imagen = compra.getImagenFactura();
            if (imagen == null || imagen.length == 0) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(imagen.length);
            return ResponseEntity.ok().headers(headers).body(imagen);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
