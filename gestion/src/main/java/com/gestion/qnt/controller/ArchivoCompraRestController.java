package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.model.ArchivoCompra;
import com.gestion.qnt.model.Compra;
import com.gestion.qnt.model.enums.TipoDocumentoCompra;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ICompraBusiness;
import com.gestion.qnt.repository.ArchivoCompraRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/compras/{compraId}/archivos")
public class ArchivoCompraRestController {

    private final ICompraBusiness compraBusiness;
    private final ArchivoCompraRepository archivoRepo;

    public ArchivoCompraRestController(ICompraBusiness compraBusiness,
                                       ArchivoCompraRepository archivoRepo) {
        this.compraBusiness = compraBusiness;
        this.archivoRepo = archivoRepo;
    }

    /** Lista los metadatos de archivos de una compra (sin bytes). */
    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ArchivoCompra>> list(@PathVariable Long compraId) {
        try {
            compraBusiness.load(compraId); // valida que exista
            return ResponseEntity.ok(archivoRepo.findByCompraId(compraId));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Sube un archivo adjunto a la compra. */
    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> upload(@PathVariable Long compraId,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam("tipo") TipoDocumentoCompra tipo) {
        try {
            Compra compra = compraBusiness.load(compraId);

            ArchivoCompra archivo = new ArchivoCompra();
            archivo.setCompra(compra);
            archivo.setTipoDocumento(tipo);
            archivo.setNombreArchivo(file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "archivo");
            archivo.setContentType(file.getContentType());
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setContenido(file.getBytes());

            ArchivoCompra saved = archivoRepo.save(archivo);
            // Limpiar contenido del objeto antes de devolver (no serializar bytes)
            saved.setContenido(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /** Descarga el contenido de un archivo. */
    @GetMapping("/{archivoId}")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable Long compraId,
                                           @PathVariable Long archivoId) {
        try {
            compraBusiness.load(compraId);
            ArchivoCompra archivo = archivoRepo.findByIdAndCompraId(archivoId, compraId)
                    .orElseThrow(() -> new NotFoundException("Archivo no encontrado"));

            String ct = archivo.getContentType();
            MediaType mediaType = ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(archivo.getContenido().length);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(archivo.getNombreArchivo(), StandardCharsets.UTF_8)
                    .build());

            return ResponseEntity.ok().headers(headers).body(archivo.getContenido());
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Elimina un archivo adjunto. */
    @DeleteMapping("/{archivoId}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long compraId,
                                       @PathVariable Long archivoId) {
        try {
            compraBusiness.load(compraId);
            ArchivoCompra archivo = archivoRepo.findByIdAndCompraId(archivoId, compraId)
                    .orElseThrow(() -> new NotFoundException("Archivo no encontrado"));
            archivoRepo.delete(archivo);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
