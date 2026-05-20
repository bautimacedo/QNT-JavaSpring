package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta de una inspección AIB. Las URLs en el campo `archivos`
 * apuntan al endpoint GET /api/qnt/v1/inspecciones/aib/{id}/archivo/{tipo}
 * del propio backend, que hace 302 redirect a una presigned URL de S3.
 */
public class InspeccionAibDTO {

    // ── Identidad ──────────────────────────────────────────────────────
    public Long id;
    public String aibId;
    public String aibNombre;
    public Long pozoId;
    public String pozoNombre;
    public String modelo;
    public String notas;
    public LocalDateTime timestamp;
    public LocalDateTime fechaRegistro;

    /** Estado de la bomba: ON | OFF | INDETERMINADO. */
    public String estado;
    public Double gpm;

    // ── Secciones de métricas ──────────────────────────────────────────
    /** Tiempos de ciclo de la bomba. null si la bomba está OFF o INDETERMINADO. */
    public TiemposCicloDTO tiemposCiclo;
    /** Velocidad instantánea derivada. null si OFF/INDETERMINADO o el AIB no tiene carrera_in. */
    public VelocidadDTO velocidad;
    /** Aceleración instantánea. null si OFF/INDETERMINADO o el AIB no tiene carrera_in. */
    public AceleracionDTO aceleracion;
    /** Conversión píxeles ↔ pulgadas. null si el AIB no tiene carrera_in configurada. */
    public ConversionDTO conversion;
    /** Metadatos del video procesado. Siempre presente. */
    public VideoDTO video;

    /** Mapa tipo→URL relativa. Solo contiene las keys de archivos que la inspección tiene en S3. */
    public Map<String, String> archivos;

    // ── Tipos anidados ─────────────────────────────────────────────────

    public static class TiemposCicloDTO {
        public Double subidaS;
        public Double bajadaS;
        public Double subidaInS;
        public Double bajadaInS;
        public Double ratio;
        public Double confianza;
    }

    public static class VelocidadDTO {
        public Double velMaxInS;
        public Double velRmsInS;
        public Double confianza;
    }

    public static class AceleracionDTO {
        public Double acelMaxInS2;
    }

    public static class ConversionDTO {
        public Double carreraIn;
        public Double carreraPx;
        public Double scaleInPerPx;
        public Double confianza;
    }

    public static class VideoDTO {
        public String nombre;
        public Double fps;
        public Double duracionSegundos;
        public Integer framesConDeteccion;
        public Integer framesTotales;
        public Double coberturaPorcentaje;
    }
}
