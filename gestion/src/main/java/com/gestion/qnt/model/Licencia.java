package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /** Piloto al que pertenece esta licencia (para licencias ANAC / mi-perfil). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piloto_id")
    private Usuario piloto;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column
    private LocalDate caducidad;

    @Column
    private String version;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Imagen/documento de la licencia. No se serializa en JSON. */
    @Lob
    @Column(name = "imagen", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagen;
}
