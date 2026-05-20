package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aibs")
@Getter
@Setter
public class Aib {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aib_id", nullable = false, unique = true)
    private String aibId;

    @Column
    private String nombre;

    /** Modelo del equipo (ej: "Lufkin C-912-365-168"). Se actualiza desde el payload del pipeline AIB. */
    @Column(length = 200)
    private String modelo;

    /** Notas libres sobre el equipo. Se actualiza desde el payload del pipeline AIB. */
    @Column(columnDefinition = "TEXT")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "pozo_id")
    private Pozo pozo;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}
