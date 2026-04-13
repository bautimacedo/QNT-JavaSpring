package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.CategoriaMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.PrioridadMision;

/**
 * Body de entrada para crear o actualizar una Mision.
 */
public class MisionRequest {

    public String nombre;
    public String descripcion;
    public String observaciones;
    public String linkRtsp;
    public CategoriaMision categoria;
    public PrioridadMision prioridad;
    public EstadoMision estado;

    // IDs de las relaciones
    public Long pilotoId;
    public Long dronId;    // nullable
    public Long dockId;    // nullable
    public Long pozoId;    // nullable

    // FlytBase webhook (EFO)
    public String webhookUrl;
    public String webhookBearer;
}
