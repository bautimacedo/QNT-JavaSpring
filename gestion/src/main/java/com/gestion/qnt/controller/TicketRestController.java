package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.CreateTicketRequest;
import com.gestion.qnt.controller.dto.TicketResponse;
import com.gestion.qnt.controller.dto.UpdateTicketEstadoRequest;
import com.gestion.qnt.model.Ticket;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/tickets")
public class TicketRestController {

    private final TicketService ticketService;

    public TicketRestController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketResponse>> list(@AuthenticationPrincipal Usuario authUser) {
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
    public ResponseEntity<?> create(@RequestBody CreateTicketRequest req,
                                    @AuthenticationPrincipal Usuario authUser) {
        try {
            Ticket ticket = ticketService.crearTicket(req, authUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEstado(@PathVariable Long id,
                                          @RequestBody UpdateTicketEstadoRequest req,
                                          @AuthenticationPrincipal Usuario authUser) {
        try {
            Ticket ticket = ticketService.actualizarEstado(id, req, authUser);
            return ResponseEntity.ok(TicketResponse.from(ticket));
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
