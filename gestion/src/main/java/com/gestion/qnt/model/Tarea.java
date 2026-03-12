package com.gestion.qnt.model;

import com.gestion.qnt.model.enums.EstadoTarea;
import com.gestion.qnt.model.enums.PrioridadTarea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas")
@Getter
@Setter
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTarea estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadTarea prioridad;

    /** Fecha límite (opcional) */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    /** Usuario al que está asignada la tarea (opcional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_a_id")
    private Usuario asignadoA;

    /** Usuario que creó la tarea */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null)    estado    = EstadoTarea.PENDIENTE;
        if (prioridad == null) prioridad = PrioridadTarea.MEDIA;
    }
}
