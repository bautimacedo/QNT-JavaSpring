package com.gestion.qnt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reportes_actividad")
@Getter
@Setter
public class ReporteActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoReporte tipo;

    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "bytea")
    @JsonIgnore
    private byte[] contenido;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
