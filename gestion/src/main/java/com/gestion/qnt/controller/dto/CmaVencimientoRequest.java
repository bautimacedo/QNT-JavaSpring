package com.gestion.qnt.controller.dto;

import java.time.LocalDate;

/**
 * Body para PUT /mi-perfil/cma (actualizar fecha vencimiento CMA).
 */
public record CmaVencimientoRequest(
        LocalDate vencimiento
) {}
