package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.TipoEquipoMapa;

import java.math.BigDecimal;
import java.time.Instant;
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
        LocalDate ultimoMantenimiento,
        String siteNombre,
        String numeroSerie,
        // Telemetría MQTT — Dron
        Integer bateriaPorc,
        BigDecimal bateriaTempC,
        Boolean droneEnDock,
        // Batería instalada — Dron
        String bateriaNombre,
        Integer bateriaCiclos,
        // Telemetría MQTT — Dock
        BigDecimal temperaturaAmbiente,
        BigDecimal velocidadViento,
        // Común
        Instant ultimaTelemetria
) {}
