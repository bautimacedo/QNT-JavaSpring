package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.Ticket;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoTicket;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String titulo,
        String descripcion,
        EstadoTicket estado,
        UsuarioResumen autor,
        UsuarioResumen resolvedBy,
        String notaResolucion,
        Instant createdAt,
        Instant updatedAt
) {
    public record UsuarioResumen(Long id, String nombre, String apellido, String email) {
        public static UsuarioResumen from(Usuario u) {
            if (u == null) return null;
            return new UsuarioResumen(u.getId(), u.getNombre(), u.getApellido(), u.getEmail());
        }
    }

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getEstado(),
                UsuarioResumen.from(t.getAutor()),
                UsuarioResumen.from(t.getResolvedBy()),
                t.getNotaResolucion(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
