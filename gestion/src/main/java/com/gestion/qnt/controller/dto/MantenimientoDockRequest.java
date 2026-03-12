package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;

public class MantenimientoDockRequest {

    public Long dockId;                    // requerido
    public Long usuarioId;                 // requerido
    public LocalDateTime fechaMantenimiento; // requerido
    public String observaciones;
    public String fotos;
}
