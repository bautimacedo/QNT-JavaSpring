package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.*;
import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.service.GeminiService;
import com.gestion.qnt.service.RegistroHoraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/horas")
@PreAuthorize("hasRole('ADMIN')")
public class RegistroHoraRestController {

    private final RegistroHoraService service;
    private final GeminiService geminiService;
    private final IUsuarioBusiness usuarioBusiness;

    public RegistroHoraRestController(RegistroHoraService service,
                                      GeminiService geminiService,
                                      IUsuarioBusiness usuarioBusiness) {
        this.service = service;
        this.geminiService = geminiService;
        this.usuarioBusiness = usuarioBusiness;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<RegistroHoraResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.listar(desde, hasta).stream().map(RegistroHoraResponse::from).toList());
    }

    @GetMapping("/resumen")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ResumenHorasResponse>> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.resumen(desde, hasta));
    }

    @PostMapping("/asistente")
    public ResponseEntity<List<BorradorHora>> asistente(@RequestBody AsistenteHorasRequest req) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        return ResponseEntity.ok(geminiService.parsearRegistros(req.texto(), hoy));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(Authentication authentication, @RequestBody CreateRegistroHoraRequest req) {
        Usuario autor = resolverUsuario(authentication);
        if (autor == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RegistroHora r = service.crear(req, autor);
            return ResponseEntity.status(HttpStatus.CREATED).body(RegistroHoraResponse.from(r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id,
                                    @RequestBody UpdateRegistroHoraRequest req) {
        Usuario solicitante = resolverUsuario(authentication);
        if (solicitante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RegistroHora r = service.actualizar(id, req, solicitante);
            return ResponseEntity.ok(RegistroHoraResponse.from(r));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        Usuario solicitante = resolverUsuario(authentication);
        if (solicitante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            service.eliminar(id, solicitante);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/ampliar")
    public ResponseEntity<AmpliarDescripcionResponse> ampliar(@RequestBody AmpliarDescripcionRequest req) {
        String ampliado = geminiService.ampliarDescripcion(req.texto());
        return ResponseEntity.ok(new AmpliarDescripcionResponse(ampliado));
    }

    private Usuario resolverUsuario(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return null;
        }
        try {
            return usuarioBusiness.load(authUser.getId());
        } catch (NotFoundException | BusinessException e) {
            return null;
        }
    }
}
