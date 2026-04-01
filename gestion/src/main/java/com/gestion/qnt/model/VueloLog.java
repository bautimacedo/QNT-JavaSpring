package com.gestion.qnt.model;

import com.gestion.qnt.model.enums.TipoEventoVuelo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Registro de eventos de vuelo provenientes de FlytBase vía n8n.
 * n8n parsea los emails de FlytBase e inserta filas directamente en esta tabla.
 * La columna event_id actúa como clave de deduplicación.
 */
@Entity
@Table(name = "vuelos_log")
@Getter
@Setter
public class VueloLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID único del evento en FlytBase (ej: KgwLnCla). Evita duplicados. */
    @Column(name = "event_id", unique = true, length = 50)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoVuelo evento;

    /** INFO / CAUTION / CRITICAL */
    @Column(length = 20)
    private String severidad;

    @Column(name = "nombre_dron", length = 100)
    private String nombreDron;

    @Column(name = "nombre_dock", length = 100)
    private String nombreDock;

    @Column(length = 50)
    private String site;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    /** Altitud en metros al momento del evento. */
    @Column(precision = 10, scale = 2)
    private BigDecimal altitud;

    /** Nivel de batería del dron en porcentaje (0-100). */
    @Column
    private Integer bateria;

    /** Nombre del piloto extraído de "Flight details" del email. */
    @Column(length = 150)
    private String piloto;

    /** Campo completo "Flight details" del email (ej: "Scheduled Mission (efo 19) - Patricio Maioli"). */
    @Column(name = "detalle_vuelo", length = 300)
    private String detalleVuelo;

    /** Timestamp original del evento según FlytBase (GMT). */
    @Column(name = "timestamp_flytbase")
    private Instant timestampFlytbase;

    /**
     * true si n8n detectó que este aterrizaje ocurrió menos de 1 minuto después
     * del despegue correspondiente (indica que el dron no pudo volar).
     * También true para eventos FALLA_DESPEGUE y DESPEGUE_FALLIDO.
     */
    @Column(name = "despegue_fallido")
    private Boolean despegueFallido = false;

    /** Fecha en que n8n insertó el registro. */
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
        if (despegueFallido == null) despegueFallido = false;
    }
}
