package com.gestion.qnt.model;

import com.gestion.qnt.model.enums.CategoriaMision;
import com.gestion.qnt.model.enums.PrioridadMision;
import com.gestion.qnt.model.enums.TipoRecurrencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "programacion_misiones")
@Getter
@Setter
@NoArgsConstructor
public class ProgramacionMision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private CategoriaMision categoria;

    @Enumerated(EnumType.STRING)
    private PrioridadMision prioridad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurrencia", nullable = false)
    private TipoRecurrencia tipoRecurrencia;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programacion_dias_semana",
            joinColumns = @JoinColumn(name = "programacion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia")
    private Set<DayOfWeek> diasSemana = new HashSet<>();

    @Column(name = "dia_mes")
    private Integer diaMes;

    @Column(name = "intervalo_dias")
    private Integer intervaloDias;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mision_plantilla_id")
    private Mision misionPlantilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dron_id")
    private Dron dron;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piloto_id")
    private Usuario piloto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id")
    private Dock dock;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "webhook_bearer", columnDefinition = "TEXT")
    private String webhookBearer;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "prox_ejecucion")
    private LocalDateTime proxEjecucion;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
