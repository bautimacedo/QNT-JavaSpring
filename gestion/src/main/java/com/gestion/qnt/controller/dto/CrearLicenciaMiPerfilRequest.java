package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/**
 * Body para POST /mi-perfil/licencias (crear licencia del piloto actual).
 */
public record CrearLicenciaMiPerfilRequest(
        String nombre,
        String numLicencia,
        LocalDate fechaCompra,
        LocalDate caducidad,
        String version,
        Boolean activo
) {}
