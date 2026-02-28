package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "seguros")
@Getter
@Setter
public class Seguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aseguradora;

    @Column(name = "numero_poliza")
    private String numeroPoliza;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Lob
    @Column
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;
}
