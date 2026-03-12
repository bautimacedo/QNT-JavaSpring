package com.gestion.qnt.controller.dto;

import com.gestion.qnt.model.enums.EstadoTarea;
import com.gestion.qnt.model.enums.PrioridadTarea;

import java.time.LocalDate;

/**
 * Body de entrada para crear o actualizar una Tarea.
 */
public class TareaRequest {

    public String titulo;
    public String descripcion;
    public EstadoTarea estado;
    public PrioridadTarea prioridad;
    public LocalDate fechaVencimiento;

    /** ID del usuario al que se asigna (nullable) */
    public Long asignadoAId;

    /** ID del usuario que crea (nullable — se puede inferir del contexto de seguridad) */
    public Long creadoPorId;
}
