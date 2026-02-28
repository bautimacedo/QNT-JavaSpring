package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drones")
@Getter
@Setter
public class Dron {

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

    @Column(name = "dias_para_mantenimiento")
    private Integer diasParaMantenimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_mantenimiento_id")
    private MantenimientoDron ultimoMantenimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguro_id")
    private Seguro seguro;

    @Column
    private String garantia;

    @Column(name = "licencia_anac")
    private String licenciaAnac;

    @Column(name = "cantidad_vuelos")
    private Integer cantidadVuelos;

    @Column(name = "cantidad_minutos_volados")
    private Integer cantidadMinutosVolados;

    @Lob
    @Column
    private String incidentes;

    @Column(name = "ultimo_vuelo")
    private Instant ultimoVuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id", unique = true)
    private Dock dock;

    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bateria> baterias = new ArrayList<>();

    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Helice> helices = new ArrayList<>();

    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MantenimientoDron> mantenimientos = new ArrayList<>();

    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY)
    private List<Mision> misiones = new ArrayList<>();
}
