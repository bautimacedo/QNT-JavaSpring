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

    @Column(name = "caducidad")
    private LocalDate caducidad;

    @Column(name = "imagen_cma", nullable = true, columnDefinition = "bytea")
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenCma;

    @Column(name = "content_type_cma", length = 100)
    private String contentTypeCma;

    @Column(name = "imagen_certificado_idoneidad", nullable = true, columnDefinition = "bytea")
    @Basic(optional = true)
    @JsonIgnore
    private byte[] imagenCertificadoIdoneidad;

    @Column(name = "content_type_cert_idoneidad", length = 100)
    private String contentTypeCertIdoneidad;

    @Column(nullable = false)
    private Boolean activo = true;
}
