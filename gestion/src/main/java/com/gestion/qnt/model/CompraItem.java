package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.qnt.model.enums.TipoCompra;
import com.gestion.qnt.model.enums.TipoEquipo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_items")
@Getter
@Setter
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    @JsonIgnore
    private Compra compra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_compra", nullable = false)
    private TipoCompra tipoCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_equipo")
    private TipoEquipo tipoEquipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(precision = 19, scale = 4)
    private BigDecimal importe;
}
