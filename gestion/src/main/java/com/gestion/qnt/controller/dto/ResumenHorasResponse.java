package com.gestion.qnt.controller.dto;

import java.math.BigDecimal;

public record ResumenHorasResponse(
        Long autorId,
        String nombre,
        String apellido,
        BigDecimal totalHoras,
        long cantidadRegistros
) {}
