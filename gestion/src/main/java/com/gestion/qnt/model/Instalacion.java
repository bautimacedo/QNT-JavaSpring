package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "instalaciones")
@Getter
@Setter
public class Instalacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Lob
    @Column
    private String observaciones;

    @Column
    private String fotos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id")
    private Dock dock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dron_id")
    private Dron dron;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bateria_id")
    private Bateria bateria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helice_id")
    private Helice helice;
}
