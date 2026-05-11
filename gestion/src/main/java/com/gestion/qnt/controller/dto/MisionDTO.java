package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.CategoriaMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.PrioridadMision;

import java.time.LocalDateTime;

/**
 * DTO de Mision para respuestas REST.
 * Evita serializar asociaciones LAZY y expone sólo los ids/nombres necesarios.
 */
public class MisionDTO {

    public Long id;
    public String nombre;
    public String descripcion;
    public String observaciones;
    public String linkRtsp;
    public CategoriaMision categoria;
    public PrioridadMision prioridad;
    public EstadoMision estado;
    public LocalDateTime fechaCreacion;
    public LocalDateTime ultimaEjecucion;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public Long duracionMinutos;

    // Piloto
    public Long pilotoId;
    public String pilotoNombre;

    // Dron (nullable)
    public Long dronId;
    public String dronNombre;

    // Dock (nullable)
    public Long dockId;
    public String dockNombre;
    public Double dockLat;
    public Double dockLon;

    // FlytBase webhook (EFO) — bearer nunca se expone en la respuesta
    public String webhookUrl;

    // FlightHub 2 (CAM)
    public String flightHubWaylineUuid;
    public String site;            // "EFO" o "CAM"
    public Double distanciaMetros;
    public Integer waypointCount;
    public String coordenadasJson; // JSON array [{lat,lon},...]

    public LocalDateTime fechaProgramada;
    public Long programacionId;
}
