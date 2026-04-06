package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MantenimientoDronRequest {

    public Long dronId;                    // requerido
    public Long usuarioId;                 // requerido
    public LocalDateTime fechaMantenimiento; // requerido
    public String tipoMantenimiento;
    public String checklist;
    public String observaciones;
    public String fotos;
    public Long bateriaViejaId;
    public Long bateriaNuevaId;
    public List<Long> helicesViejasIds;
    public List<Long> helicesNuevasIds;
}
