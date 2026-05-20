package com.gestion.qnt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Resultado de una inspección automática de un AIB. El pipeline externo procesa
 * un video y sube los outputs (imágenes, txt, video) a S3, luego POSTea este
 * objeto al backend con las S3 keys. Los archivos en sí no se almacenan acá
 * — se sirven on-demand vía presigned URLs.
 */
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

    /** Estado de la bomba: ON | OFF | INDETERMINADO. */
    @Column(nullable = false, length = 20)
    private String estado;

    /** Galones por minuto. Null si OFF o INDETERMINADO. */
    @Column
    private Double gpm;

    // ── Tiempos de ciclo (payload: tiempos_ciclo) ─────────────────────
    @Column(name = "tc_subida_s")    private Double tcSubidaS;
    @Column(name = "tc_bajada_s")    private Double tcBajadaS;
    @Column(name = "tc_subida_in_s") private Double tcSubidaInS;
    @Column(name = "tc_bajada_in_s") private Double tcBajadaInS;
    @Column(name = "tc_ratio")       private Double tcRatio;
    @Column(name = "tc_confianza")   private Double tcConfianza;

    // ── Velocidad (payload: velocidad) ─────────────────────────────────
    @Column(name = "vel_max_in_s")  private Double velMaxInS;
    @Column(name = "vel_rms_in_s")  private Double velRmsInS;
    @Column(name = "vel_confianza") private Double velConfianza;

    // ── Aceleración (payload: aceleracion) ─────────────────────────────
    @Column(name = "acel_max_in_s2") private Double acelMaxInS2;

    // ── Conversión px→in (payload: conversion) ─────────────────────────
    @Column(name = "conv_carrera_in")     private Double convCarreraIn;
    @Column(name = "conv_carrera_px")     private Double convCarreraPx;
    @Column(name = "conv_scale_in_per_px") private Double convScaleInPerPx;
    @Column(name = "conv_confianza")      private Double convConfianza;

    // ── Metadatos del video (payload: video) ───────────────────────────
    @Column(name = "video_nombre", length = 255)
    private String videoNombre;
    @Column(name = "video_fps")
    private Double videoFps;
    @Column(name = "video_duracion_segundos")
    private Double videoDuracionSegundos;
    @Column(name = "video_frames_con_deteccion")
    private Integer videoFramesConDeteccion;
    @Column(name = "video_frames_totales")
    private Integer videoFramesTotales;
    @Column(name = "video_cobertura_porcentaje")
    private Double videoCoberturaPorcentaje;

    // ── S3 bucket (payload: s3_bucket) ─────────────────────────────────
    /** Bucket donde viven todos los archivos de esta inspección. Si es null, usar el default. */
    @Column(name = "s3_bucket", length = 200)
    private String s3Bucket;

    // ── S3 keys de archivos (payload: archivos.*) ─────────────────────
    // Obligatorios: video, captura, detecciones, grafico_detecciones_raw
    @Column(name = "s3_key_video", length = 1024)
    private String s3KeyVideo;
    @Column(name = "s3_key_captura", length = 1024)
    private String s3KeyCaptura;
    @Column(name = "s3_key_grafico_detecciones_raw", length = 1024)
    private String s3KeyGraficoDeteccionesRaw;
    @Column(name = "s3_key_detecciones", length = 1024)
    private String s3KeyDetecciones;

    // Opcionales: solo si la bomba está ON y el AIB tiene carrera_in
    @Column(name = "s3_key_grafico_tiempos_ciclo", length = 1024)
    private String s3KeyGraficoTiemposCiclo;
    @Column(name = "s3_key_grafico_posicion_pulgadas", length = 1024)
    private String s3KeyGraficoPosicionPulgadas;
    @Column(name = "s3_key_grafico_velocidad_pulgadas", length = 1024)
    private String s3KeyGraficoVelocidadPulgadas;
    @Column(name = "s3_key_grafico_aceleracion_pulgadas", length = 1024)
    private String s3KeyGraficoAceleracionPulgadas;
    @Column(name = "s3_key_posiciones_pulgadas", length = 1024)
    private String s3KeyPosicionesPulgadas;
    @Column(name = "s3_key_posiciones_tam", length = 1024)
    private String s3KeyPosicionesTam;
    @Column(name = "s3_key_velocidad_aceleracion_pulgadas", length = 1024)
    private String s3KeyVelocidadAceleracionPulgadas;
}
