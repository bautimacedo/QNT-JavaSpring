package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "licencias")
@Getter
@Setter
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "num_licencia")
    private String numLicencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column
    private LocalDate caducidad;

    @Column
    private String version;

    @Column(nullable = false)
    private Boolean activo = true;
}
