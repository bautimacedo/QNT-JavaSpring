package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "licencias_anac")
@Getter
@Setter
public class LicenciaANAC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piloto_id", nullable = false)
    private Usuario piloto;

    @Column(name = "fecha_vencimiento_cma")
    private LocalDate fechaVencimientoCma;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column
    private LocalDate caducidad;

    @Lob
    @Column(name = "imagen_cma", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenCma;

    @Lob
    @Column(name = "imagen_certificado_idoneidad", nullable = true)
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenCertificadoIdoneidad;

    @Column(nullable = false)
    private Boolean activo = true;
}
