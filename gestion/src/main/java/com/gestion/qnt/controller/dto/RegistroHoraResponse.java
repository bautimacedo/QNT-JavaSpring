package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.Usuario;

import java.time.Instant;
import java.time.LocalDate;

public record RegistroHoraResponse(
        Long id,
        UsuarioResumen autor,
        LocalDate fecha,
        String descripcion,
        Instant createdAt,
        Instant updatedAt
) {
    public record UsuarioResumen(Long id, String nombre, String apellido, String email) {
        public static UsuarioResumen from(Usuario u) {
            if (u == null) return null;
            return new UsuarioResumen(u.getId(), u.getNombre(), u.getApellido(), u.getEmail());
        }
    }

    public static RegistroHoraResponse from(RegistroHora r) {
        return new RegistroHoraResponse(
                r.getId(),
                UsuarioResumen.from(r.getAutor()),
                r.getFecha(),
                r.getDescripcion(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
