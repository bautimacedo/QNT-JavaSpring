package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateTicketRequest;
import com.gestion.qnt.controller.dto.TicketResponse;
import com.gestion.qnt.controller.dto.UpdateTicketEstadoRequest;
import com.gestion.qnt.model.Ticket;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/tickets")
public class TicketRestController {

    private final TicketService ticketService;
    private final IUsuarioBusiness usuarioBusiness;

    public TicketRestController(TicketService ticketService, IUsuarioBusiness usuarioBusiness) {
        this.ticketService = ticketService;
        this.usuarioBusiness = usuarioBusiness;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketResponse>> list(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isAdmin = authUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<Ticket> tickets = isAdmin
                ? ticketService.listarTodos()
                : ticketService.listarPorAutor(authUser.getId());
        return ResponseEntity.ok(tickets.stream().map(TicketResponse::from).toList());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(Authentication authentication,
                                    @RequestBody CreateTicketRequest req) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo determinar el usuario autenticado");
        }
        try {
            Usuario autor = usuarioBusiness.load(authUser.getId());
            Ticket ticket = ticketService.crearTicket(req, autor);
            return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(ticket));
        } catch (NotFoundException | BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo determinar el usuario autenticado");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEstado(Authentication authentication,
                                          @PathVariable Long id,
                                          @RequestBody UpdateTicketEstadoRequest req) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo determinar el usuario autenticado");
        }
        try {
            Usuario admin = usuarioBusiness.load(authUser.getId());
            Ticket ticket = ticketService.actualizarEstado(id, req, admin);
            return ResponseEntity.ok(TicketResponse.from(ticket));
        } catch (NotFoundException | BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No se pudo determinar el usuario autenticado");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            ticketService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
