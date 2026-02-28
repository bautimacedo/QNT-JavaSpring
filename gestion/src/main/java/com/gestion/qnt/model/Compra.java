package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(name = "numero_factura")
    private String numeroFactura;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal importe;

    @Column(nullable = false, length = 10)
    private String moneda = "ARS";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compra", nullable = false)
    private TipoCompra tipoCompra;

    @Lob
    @Column
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Lob
    @Column
    private String observaciones;
}
