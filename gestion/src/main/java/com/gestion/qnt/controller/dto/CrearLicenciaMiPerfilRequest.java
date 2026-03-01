package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/**
 * Body para POST /mi-perfil/licencias (crear licencia ANAC del piloto actual).
 */
public record CrearLicenciaMiPerfilRequest(
        LocalDate fechaVencimientoCma,
        LocalDate fechaEmision,
        LocalDate caducidad,
        Boolean activo
) {}
