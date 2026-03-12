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

    // Piloto
    public Long pilotoId;
    public String pilotoNombre;

    // Dron (nullable)
    public Long dronId;
    public String dronNombre;

    // Dock (nullable)
    public Long dockId;
    public String dockNombre;
}
