package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "imagen_poliza", nullable = true, columnDefinition = "bytea")
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenPoliza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    public boolean isTieneImagen() {
        return imagenPoliza != null && imagenPoliza.length > 0;
    }
}
