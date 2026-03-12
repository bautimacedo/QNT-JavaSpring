package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** Si la compra tiene IVA incluido en el total. Cuando true, ivaPorcentaje es obligatorio. */
    @Column(name = "tiene_iva")
    private Boolean tieneIva = false;

    /** Porcentaje de IVA (ej: 21.00). Solo aplica cuando tieneIva = true. */
    @Column(name = "iva_porcentaje", precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

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
    @Column(name = "imagen_factura", nullable = true, columnDefinition = "bytea")
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenFactura;

    /**
     * Subtotal (base imponible) cuando tieneIva es true: importe / (1 + ivaPorcentaje/100).
     * No persistido; calculado para la respuesta de la API.
     */
    @Transient
    public BigDecimal getSubtotal() {
        if (!Boolean.TRUE.equals(tieneIva) || ivaPorcentaje == null || ivaPorcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (importe == null) {
            return null;
        }
        BigDecimal divisor = BigDecimal.ONE.add(ivaPorcentaje.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return importe.divide(divisor, 4, RoundingMode.HALF_UP);
    }
}
