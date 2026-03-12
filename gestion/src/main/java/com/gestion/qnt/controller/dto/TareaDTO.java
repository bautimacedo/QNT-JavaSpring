package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.EstadoTarea;
import com.gestion.qnt.model.enums.PrioridadTarea;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de Tarea para respuestas REST.
 */
public class TareaDTO {

    public Long id;
    public String titulo;
    public String descripcion;
    public EstadoTarea estado;
    public PrioridadTarea prioridad;
    public LocalDate fechaVencimiento;
    public LocalDateTime fechaCreacion;
    public LocalDateTime fechaCompletada;

    // Asignado a (nullable)
    public Long asignadoAId;
    public String asignadoANombre;

    // Creado por (nullable)
    public Long creadoPorId;
    public String creadoPorNombre;

    /** true si fechaVencimiento < hoy y estado != COMPLETADA */
    public boolean vencida;
}
