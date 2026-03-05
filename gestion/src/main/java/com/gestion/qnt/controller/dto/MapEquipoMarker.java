package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.TipoEquipoMapa;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para un marcador de equipo en el mapa.
 * Solo se incluyen equipos con latitud y longitud no nulas.
 */
public record MapEquipoMarker(
        TipoEquipoMapa tipoEquipo,
        Long id,
        String nombre,
        BigDecimal latitud,
        BigDecimal longitud,
        BigDecimal altitud,
        Estado estado,
        /** Fecha del último mantenimiento (Dock/Dron); null para el resto. */
        LocalDate ultimoMantenimiento,
        /** Nombre del site (Dock); null para el resto. */
        String siteNombre,
        /** Número de serie si existe. */
        String numeroSerie
) {}
