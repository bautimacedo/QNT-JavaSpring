package com.gestion.qnt.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body para PUT /mi-perfil/cambio-password.
 */
public record CambioPasswordMiPerfilRequest(
        @NotBlank(message = "oldPassword es obligatoria")
        String oldPassword,

        @NotBlank(message = "newPassword es obligatoria")
        @Size(min = 6, message = "newPassword debe tener al menos 6 caracteres")
        String newPassword
) {}
