package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "logs")
@Getter
@Setter
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidad_tipo", nullable = false)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private String tipo;

    @Column(columnDefinition = "text")
    private String detalle;

    /** Minutos de vuelo registrados manualmente en este log. */
    @Column(name = "minutos_vuelo")
    private Integer minutosVuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
