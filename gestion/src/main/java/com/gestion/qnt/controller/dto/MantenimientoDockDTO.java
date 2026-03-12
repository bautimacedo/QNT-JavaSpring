package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;

public class MantenimientoDockDTO {

    public Long id;

    // Dock
    public Long dockId;
    public String dockNombre;
    public String dockModelo;

    // Técnico responsable
    public Long usuarioId;
    public String usuarioNombre;

    public LocalDateTime fechaMantenimiento;

    public String observaciones;

    public String fotos;
}
