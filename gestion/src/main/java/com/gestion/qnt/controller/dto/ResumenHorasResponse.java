package com.gestion.qnt.controller.dto;

public record ResumenHorasResponse(
        Long autorId,
        String nombre,
        String apellido,
        long cantidadRegistros
) {}
