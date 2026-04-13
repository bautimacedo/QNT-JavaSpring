package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import com.gestion.qnt.model.enums.Estado;

@Entity
@Table(name = "helices")
@Getter
@Setter
public class Helice {

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

    @Column(name = "dias_uso")
    private Integer diasUso;

    @Column(name = "cantidad_vuelos")
    private Integer cantidadVuelos;

    @Column(name = "cantidad_minutos_volados")
    private Integer cantidadMinutosVolados;

    @Column(name = "fecha_stock_activo")
    private LocalDateTime fechaStockActivo;

    @Column(name = "fecha_en_desuso")
    private LocalDateTime fechaEnDesuso;

    @Column(name = "dron_id", insertable = false, updatable = false)
    private Long dronId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dron_id")
    private Dron dron;

    /** Coordenadas opcionales para ubicación en mapa (lat/lng obligatorios para aparecer; altitud en metros). */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;
    @Column(precision = 10, scale = 2)
    private BigDecimal altitud;
}
