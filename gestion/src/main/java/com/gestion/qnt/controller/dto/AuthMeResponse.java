package com.gestion.qnt.controller.dto;

import java.util.List;

/**
 * Respuesta de GET /auth/me.
 * El frontend espera "authorities" como array de strings (ej. ["ROLE_PILOTO", "ROLE_ADMIN"]).
 */
public record AuthMeResponse(
        Long id,
        String email,
        String username,
        List<String> authorities
) {}
