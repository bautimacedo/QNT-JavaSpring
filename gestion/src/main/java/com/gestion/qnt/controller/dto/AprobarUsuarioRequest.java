package com.gestion.qnt.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body para PUT /usuarios/{id}/aprobar.
 */
public record AprobarUsuarioRequest(
        @NotBlank(message = "roleCodigo es obligatorio")
        String roleCodigo
) {}
