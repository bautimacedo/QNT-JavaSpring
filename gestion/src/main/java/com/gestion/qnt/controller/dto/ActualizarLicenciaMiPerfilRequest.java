package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/**
 * Body para PUT /mi-perfil/licencias/{id} (actualizar licencia ANAC).
 */
public record ActualizarLicenciaMiPerfilRequest(
        LocalDate fechaVencimientoCma,
        LocalDate fechaEmision,
        LocalDate caducidad,
        Boolean activo
) {}
