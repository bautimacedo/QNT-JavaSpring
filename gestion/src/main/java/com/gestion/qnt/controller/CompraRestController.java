package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateCompraRequest;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.TipoEquipo;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.service.DolarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/compras")
public class CompraRestController {

    private final ICompraBusiness compraBusiness;
    private final IUsuarioBusiness usuarioBusiness;
    private final DolarService dolarService;

    public CompraRestController(ICompraBusiness compraBusiness, IUsuarioBusiness usuarioBusiness, DolarService dolarService) {
        this.compraBusiness = compraBusiness;
        this.usuarioBusiness = usuarioBusiness;
        this.dolarService = dolarService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Compra>> list(
            @RequestParam(required = false) com.gestion.qnt.model.enums.TipoCompra tipoCompra,
            @RequestParam(required = false) Long proveedorId) {
        try {
            if (tipoCompra != null || proveedorId != null) {
                return ResponseEntity.ok(compraBusiness.listFiltered(tipoCompra, proveedorId));
            }
            return ResponseEntity.ok(compraBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USUARIO')")
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
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USUARIO')")
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateCompraRequest request) {
        if (!request.hasProveedor()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Se debe indicar proveedorId o proveedorNombre");
        }
        try {
            // Usuario que da de alta la compra: se obtiene siempre del contexto de seguridad.
            if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo determinar el usuario autenticado");
            }

            Usuario usuarioAlta = usuarioBusiness.load(authUser.getId());

            Compra created = compraBusiness.add(request);
            created.setUsuarioAlta(usuarioAlta);
            if ("USD".equals(created.getMoneda()) && created.getImporte() != null) {
                BigDecimal cotizacion = dolarService.getCotizacionOficialVenta();
                if (cotizacion != null) {
                    created.setCotizacionDolar(cotizacion);
                    created.setImporteArs(created.getImporte().multiply(cotizacion));
                }
            }
            Compra saved = compraBusiness.update(created);
            // Recargar con relaciones para evitar LazyInitializationException al serializar a JSON
            return ResponseEntity.status(HttpStatus.CREATED).body(compraBusiness.load(saved.getId()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BusinessException e) {
            // Errores de negocio (validaciones) → 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USUARIO')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateCompraRequest request) {
        if (!request.hasProveedor()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Se debe indicar proveedorId o proveedorNombre");
        }
        try {
            Compra updated = compraBusiness.update(id, request);
            if ("USD".equals(updated.getMoneda()) && updated.getImporte() != null) {
                BigDecimal cotizacion = dolarService.getCotizacionOficialVenta();
                if (cotizacion != null) {
                    updated.setCotizacionDolar(cotizacion);
                    updated.setImporteArs(updated.getImporte().multiply(cotizacion));
                    compraBusiness.update(updated);
                }
            }
            // Recargar con relaciones para evitar LazyInitializationException al serializar a JSON
            return ResponseEntity.ok(compraBusiness.load(updated.getId()));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            // Errores de negocio (validaciones) → 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/tipos-equipo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TipoEquipo[]> getTiposEquipo() {
        return ResponseEntity.ok(TipoEquipo.values());
    }

    @DeleteMapping("/{id}")
    @Transactional
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

}
