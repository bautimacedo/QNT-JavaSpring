package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import com.gestion.qnt.model.enums.Estado;

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

    @Column(columnDefinition = "TEXT")
    private String incidentes;

    @Column(name = "ultimo_vuelo")
    private Instant ultimoVuelo;

    /** Coordenadas opcionales para ubicación en mapa (lat/lng obligatorios para aparecer; altitud en metros). */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;
    @Column(precision = 10, scale = 2)
    private BigDecimal altitud;

    // Telemetría MQTT — actualizado cada 5 minutos
    @Column(name = "bateria_porc")
    private Integer bateriaPorc;

    @Column(name = "bateria_temp_c", precision = 5, scale = 2)
    private BigDecimal bateriaTempC;

    @Column(name = "drone_en_dock")
    private Boolean droneEnDock;

    @Column(name = "ultima_telemetria")
    private Instant ultimaTelemetria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id", unique = true)
    private Dock dock;

    @JsonIgnore
    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bateria> baterias = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Helice> helices = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MantenimientoDron> mantenimientos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dron", fetch = FetchType.LAZY)
    private List<Mision> misiones = new ArrayList<>();
}
