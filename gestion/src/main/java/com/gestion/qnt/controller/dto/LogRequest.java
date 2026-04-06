package com.gestion.qnt.controller.dto;

import java.time.Instant;

public class LogRequest {

    public String entidadTipo;   // ej: "DRON", "DOCK", "MISION"
    public Long entidadId;       // id de la entidad relacionada
    public Instant timestamp;    // null → se usa Instant.now()
    public String tipo;          // ej: "VUELO", "INCIDENTE", "MANTENIMIENTO"
    public String detalle;
    public Long usuarioId;
    public Integer minutosVuelo;
}
