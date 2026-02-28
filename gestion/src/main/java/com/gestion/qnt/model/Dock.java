package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "docks")
@Getter
@Setter
public class Dock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String marca;

    @Column
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @Column
    private String nombre;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "horas_uso")
    private Integer horasUso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguro_id")
    private Seguro seguro;

    @Column
    private String garantia;

    @Column(name = "ultimo_uso")
    private Instant ultimoUso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_mantenimiento_id")
    private MantenimientoDock ultimoMantenimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "dock")
    private Dron dron;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licencia_id")
    private Licencia licencia;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "dock", cascade = CascadeType.ALL, orphanRemoval = true)
    private AntenaRtk antenaRtk;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "dock", cascade = CascadeType.ALL, orphanRemoval = true)
    private AntenaStarlink antenaStarlink;

    @OneToMany(mappedBy = "dock", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MantenimientoDock> mantenimientos = new ArrayList<>();
}
