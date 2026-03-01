package com.gestion.qnt.controller.dto;

/**
 * Body para PUT /mi-perfil (actualizar datos editables del usuario actual).
 */
public record ActualizarMiPerfilRequest(
        String nombre,
        String apellido,
        String dni
) {}
