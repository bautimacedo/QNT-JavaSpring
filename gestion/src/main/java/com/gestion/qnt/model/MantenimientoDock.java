package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimiento_dock")
@Getter
@Setter
public class MantenimientoDock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dock_id", nullable = false)
    private Dock dock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_mantenimiento", nullable = false)
    private LocalDateTime fechaMantenimiento;

    @Lob
    @Column
    private String observaciones;

    @Column
    private String fotos;
}
