package com.gestion.qnt.model;

import com.gestion.qnt.model.enums.NivelAlerta;
import com.gestion.qnt.model.enums.TipoAlerta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelAlerta nivel;

    @Column(nullable = false)
    private String mensaje;

    @Column
    private String subtitulo;

    @Column(nullable = false)
    private Boolean resuelta = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    /** Tipo de entidad que generó la alerta: PILOTO, BATERIA, DRON, etc. */
    @Column(name = "entidad_tipo", length = 30)
    private String entidadTipo;

    /** ID de la entidad relacionada (FK lógica, sin constraint). */
    @Column(name = "entidad_id")
    private Long entidadId;

    /**
     * Clave de deduplicación: evita crear alertas duplicadas para la misma entidad+tipo.
     * Formato: "TIPO_entidadTipo_entidadId", ej: "CMA_PILOTO_5"
     */
    @Column(name = "clave_dedup", unique = true, length = 100)
    private String claveDedup;

    public Alerta(TipoAlerta tipo, NivelAlerta nivel, String mensaje, String subtitulo,
                  String entidadTipo, Long entidadId) {
        this.tipo        = tipo;
        this.nivel       = nivel;
        this.mensaje     = mensaje;
        this.subtitulo   = subtitulo;
        this.entidadTipo = entidadTipo;
        this.entidadId   = entidadId;
        this.resuelta    = false;
        this.fechaCreacion = LocalDateTime.now();
        if (entidadTipo != null && entidadId != null) {
            this.claveDedup = tipo.name() + "_" + entidadTipo + "_" + entidadId;
        }
    }
}
