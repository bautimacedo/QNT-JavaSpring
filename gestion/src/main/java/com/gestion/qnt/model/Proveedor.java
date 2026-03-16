package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.qnt.model.enums.CategoriaProveedor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String cuit;

    @Column
    private String contacto;

    @Column
    private String direccion;

    @Column
    private String telefono;

    @Column
    private String email;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column
    private CategoriaProveedor categoria;

    @OneToMany(mappedBy = "proveedor", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Compra> compras = new ArrayList<>();
}
