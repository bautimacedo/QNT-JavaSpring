package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspecciones_aib")
@Getter
@Setter
public class InspeccionAib {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aib_id", nullable = false)
    private Aib aib;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false)
    private String estado;

    @Column
    private Double gpm;

    // Velocidad
    @Column(name = "vel_subida_s") private Double velSubidaS;
    @Column(name = "vel_bajada_s") private Double velBajadaS;
    @Column(name = "vel_subida_in_s") private Double velSubidaInS;
    @Column(name = "vel_bajada_in_s") private Double velBajadaInS;
    @Column(name = "vel_ratio") private Double velRatio;
    @Column(name = "vel_confianza") private Double velConfianza;

    // Derivada en píxeles
    @Column(name = "derivada_vel_max_px_s") private Double derivadaVelMaxPxS;
    @Column(name = "derivada_vel_rms_px_s") private Double derivadaVelRmsPxS;
    @Column(name = "derivada_acel_max_px_s2") private Double derivadaAcelMaxPxS2;
    @Column(name = "derivada_confianza") private Double derivadaConfianza;

    // Conversión px→in
    @Column(name = "conv_carrera_in") private Double convCarreraIn;
    @Column(name = "conv_carrera_px") private Double convCarreraPx;
    @Column(name = "conv_scale_in_per_px") private Double convScaleInPerPx;
    @Column(name = "conv_confianza") private Double convConfianza;

    // Derivada en pulgadas
    @Column(name = "derivada_in_vel_max_in_s") private Double derivadaInVelMaxInS;
    @Column(name = "derivada_in_vel_rms_in_s") private Double derivadaInVelRmsInS;
    @Column(name = "derivada_in_acel_max_in_s2") private Double derivadaInAcelMaxInS2;

    // Imágenes (paths relativos al upload-dir)
    @Column(name = "captura_anotada_path") private String capturaAnotadaPath;
    @Column(name = "grafico_posicion_in_path") private String graficoPosicionInPath;
    @Column(name = "grafico_procesada_path") private String graficoProcesadaPath;
    @Column(name = "grafico_velocidad_path") private String graficoVelocidadPath;
    @Column(name = "grafico_derivada_in_path") private String graficoDerivadaInPath;
    @Column(name = "grafico_aceleracion_in_path") private String graficoAceleracionInPath;
}
