package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.gestion.qnt.model.enums.CategoriaMision;
import com.gestion.qnt.model.enums.EstadoMision;
import com.gestion.qnt.model.enums.PrioridadMision;

@Entity
@Table(name = "misiones")
@Getter
@Setter
public class Mision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "link_rtsp")
    private String linkRtsp;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria")
    private CategoriaMision categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad")
    private PrioridadMision prioridad;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "piloto_id", nullable = true)
    private Usuario piloto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id")
    private Dock dock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dron_id")
    private Dron dron;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pozo_id")
    private Pozo pozo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /** URL del webhook FlytBase para lanzar esta misión. Ej: https://api.flytbase.com/v2/integrations/webhook/{token} */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /** Bearer token (JWT) de autorización para el webhook FlytBase. Usamos TEXT porque los JWT son largos. */
    @Column(name = "webhook_bearer", columnDefinition = "TEXT")
    private String webhookBearer;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programacion_id")
    private ProgramacionMision programacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMision estado;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoMision.PLANIFICADA;
        }
        if (prioridad == null) {
            prioridad = PrioridadMision.MEDIA;
        }
    }
}
