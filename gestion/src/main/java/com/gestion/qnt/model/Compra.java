package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.qnt.model.enums.MetodoPago;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.enums.TipoEquipo;

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

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal importe;

    @Column(nullable = false, length = 10)
    private String moneda = "ARS";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compra", nullable = false)
    private TipoCompra tipoCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "compania_tarjeta", length = 50)
    private String companiaTarjeta;

    @Column(name = "ultimos4_tarjeta", length = 4)
    private String ultimos4Tarjeta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_equipo", nullable = true)
    private TipoEquipo tipoEquipo;

    @Column(name = "descripcion_equipo", length = 255, nullable = true)
    private String descripcionEquipo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_alta_id")
    private Usuario usuarioAlta;

    /** Imagen de la factura (opcional). No se serializa en JSON para evitar payload enorme. */
    @Lob
    @Column(name = "imagen_factura", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenFactura;
}
