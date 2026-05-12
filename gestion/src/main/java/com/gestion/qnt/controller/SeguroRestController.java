package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateSeguroRequest;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.Seguro;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.model.business.interfaces.ISeguroBusiness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Seguro>> list() {
        try {
            return ResponseEntity.ok(seguroBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
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

    @PutMapping("/{id}/imagen")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> subirImagen(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            boolean isPdf  = bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46;
            boolean isJpeg = bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            boolean isPng  = bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
            if (!isPdf && !isJpeg && !isPng) {
                return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos PDF, JPEG o PNG"));
            }
            Seguro seguro = seguroBusiness.load(id);
            seguro.setImagenPoliza(bytes);
            seguroBusiness.update(seguro);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/imagen")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<byte[]> getImagen(@PathVariable Long id) {
        try {
            Seguro seguro = seguroBusiness.load(id);
            byte[] imagen = seguro.getImagenPoliza();
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
