package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;

import com.gestion.qnt.model.enums.Estado;

@Entity
@Table(name = "antenas_rtk", uniqueConstraints = @UniqueConstraint(columnNames = "dock_id"))
@Getter
@Setter
public class AntenaRtk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id", unique = true)
    private Dock dock;

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

    @Column
    private String ubicacion;

    /** Coordenadas opcionales para ubicación en mapa (lat/lng obligatorios para aparecer; altitud en metros). */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;
    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;
    @Column(precision = 10, scale = 2)
    private BigDecimal altitud;
}
