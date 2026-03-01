package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import com.gestion.qnt.model.enums.Estado;

@Entity
@Table(name = "antenas_rtk", uniqueConstraints = @UniqueConstraint(columnNames = "dock_id"))
@Getter
@Setter
public class AntenaRtk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dock_id", nullable = false, unique = true)
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
}
