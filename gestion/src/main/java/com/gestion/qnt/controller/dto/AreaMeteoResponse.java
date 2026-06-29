package com.gestion.qnt.controller.dto;

/** Área/estación meteorológica con su última lectura (para el selector y la portada). */
public record AreaMeteoResponse(
        String code,
        String nombre,
        double lat,
        double lon,
        MeteoActualResponse actual
) {}
