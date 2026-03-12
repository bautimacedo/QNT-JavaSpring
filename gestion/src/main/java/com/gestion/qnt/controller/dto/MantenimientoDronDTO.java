package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MantenimientoDronDTO {

    public Long id;

    // Dron
    public Long dronId;
    public String dronNombre;
    public String dronModelo;

    // Técnico responsable
    public Long usuarioId;
    public String usuarioNombre;

    public LocalDateTime fechaMantenimiento;

    public String observaciones;

    public String fotos;

    // Baterías intercambiadas
    public Long bateriaViejaId;
    public String bateriaViejaNombre;

    public Long bateriaNuevaId;
    public String bateriaNuevaNombre;

    // Hélices
    public List<Long> helicesViejasIds;
    public List<Long> helicesNuevasIds;
}
