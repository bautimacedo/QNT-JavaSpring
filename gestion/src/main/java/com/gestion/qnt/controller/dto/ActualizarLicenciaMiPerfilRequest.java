package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/**
 * Body para PUT /mi-perfil/licencias/{id}.
 */
public record ActualizarLicenciaMiPerfilRequest(
        String nombre,
        String numLicencia,
        LocalDate fechaCompra,
        LocalDate caducidad,
        String version,
        Boolean activo
) {}
