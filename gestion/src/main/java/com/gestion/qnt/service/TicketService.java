package com.gestion.qnt.service;

import com.gestion.qnt.controller.dto.CreateTicketRequest;
import com.gestion.qnt.controller.dto.UpdateTicketEstadoRequest;
import com.gestion.qnt.model.Ticket;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoTicket;
import com.gestion.qnt.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EmailService emailService;

    public TicketService(TicketRepository ticketRepository, EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.emailService = emailService;
    }

    public Ticket crearTicket(CreateTicketRequest req, Usuario autor) {
        if (req.titulo() == null || req.titulo().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        Ticket ticket = new Ticket();
        ticket.setTitulo(req.titulo().trim());
        ticket.setDescripcion(req.descripcion());
        ticket.setAutor(autor);
        ticket = ticketRepository.save(ticket);

        try {
            emailService.sendTicketCreatedAdmin(ticket);
        } catch (Exception e) {
            log.error("Error enviando mail de nuevo ticket #{}", ticket.getId(), e);
        }
        return ticket;
    }

    public List<Ticket> listarTodos() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Ticket> listarPorAutor(Long autorId) {
        return ticketRepository.findByAutorIdOrderByCreatedAtDesc(autorId);
    }

    public Ticket actualizarEstado(Long id, UpdateTicketEstadoRequest req, Usuario admin) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado: " + id));

        boolean resuelto = req.estado() == EstadoTicket.RESUELTO || req.estado() == EstadoTicket.CERRADO;
        ticket.setEstado(req.estado());
        ticket.setNotaResolucion(req.notaResolucion());
        ticket.setResolvedBy(resuelto ? admin : null);
        ticket.setUpdatedAt(Instant.now());
        ticket = ticketRepository.save(ticket);

        if (resuelto) {
            try {
                emailService.sendTicketResolvedToAutor(ticket);
            } catch (Exception e) {
                log.error("Error enviando mail de resolución de ticket #{}", ticket.getId(), e);
            }
        }
        return ticket;
    }

    public void eliminar(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new IllegalArgumentException("Ticket no encontrado: " + id);
        }
        ticketRepository.deleteById(id);
    }
}
