package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mantenimiento_dron")
@Getter
@Setter
public class MantenimientoDron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dron_id", nullable = false)
    private Dron dron;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bateria_vieja_id")
    private Bateria bateriaVieja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bateria_nueva_id")
    private Bateria bateriaNueva;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mantenimiento_dron_helices_viejas",
            joinColumns = @JoinColumn(name = "mantenimiento_dron_id"),
            inverseJoinColumns = @JoinColumn(name = "helice_id")
    )
    private List<Helice> helicesViejas = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mantenimiento_dron_helices_nuevas",
            joinColumns = @JoinColumn(name = "mantenimiento_dron_id"),
            inverseJoinColumns = @JoinColumn(name = "helice_id")
    )
    private List<Helice> helicesNuevas = new ArrayList<>();
}
