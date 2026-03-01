package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.gestion.qnt.model.enums.Estado;

@Entity
@Table(name = "baterias")
@Getter
@Setter
public class Bateria {

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

    @Column(name = "dias_uso")
    private Integer diasUso;

    @Column(name = "ciclos_carga")
    private Integer ciclosCarga;

    @Column(name = "fecha_stock_activo")
    private LocalDateTime fechaStockActivo;

    @Column(name = "fecha_en_desuso")
    private LocalDateTime fechaEnDesuso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dron_id")
    private Dron dron;
}
