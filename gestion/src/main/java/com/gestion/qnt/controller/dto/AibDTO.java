package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;

public class AibDTO {

    public Long id;
    public String aibId;
    public String nombre;
    public String modelo;
    public String notas;
    public Long pozoId;
    public String pozoNombre;
    public LocalDateTime fechaCreacion;

    // Última inspección resumida
    public LocalDateTime ultimaInspeccion;
    public String ultimoEstado;
    public Double ultimoGpm;
}
