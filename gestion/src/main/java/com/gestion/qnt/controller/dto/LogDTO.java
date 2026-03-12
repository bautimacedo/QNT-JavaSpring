package com.gestion.qnt.controller.dto;

import java.time.Instant;

public class LogDTO {

    public Long id;
    public String entidadTipo;
    public Long entidadId;
    public Instant timestamp;
    public String tipo;
    public String detalle;

    // Usuario que realizó la acción
    public Long usuarioId;
    public String usuarioNombre;
}
