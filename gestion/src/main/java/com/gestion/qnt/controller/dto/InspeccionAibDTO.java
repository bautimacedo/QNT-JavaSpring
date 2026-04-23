package com.gestion.qnt.controller.dto;

import java.time.LocalDateTime;

public class InspeccionAibDTO {

    public Long id;
    public String aibId;
    public String aibNombre;
    public Long pozoId;
    public String pozoNombre;
    public LocalDateTime timestamp;
    public LocalDateTime fechaRegistro;
    public String estado;
    public Double gpm;

    // Velocidad
    public Double velSubidaS;
    public Double velBajadaS;
    public Double velSubidaInS;
    public Double velBajadaInS;
    public Double velRatio;
    public Double velConfianza;

    // Derivada px
    public Double derivadaVelMaxPxS;
    public Double derivadaVelRmsPxS;
    public Double derivadaAcelMaxPxS2;
    public Double derivadaConfianza;

    // Conversión
    public Double convCarreraIn;
    public Double convCarreraPx;
    public Double convScaleInPerPx;
    public Double convConfianza;

    // Derivada in
    public Double derivadaInVelMaxInS;
    public Double derivadaInVelRmsInS;
    public Double derivadaInAcelMaxInS2;

    public String videoUrl;

    // Imágenes (URLs relativas)
    public String capturaAnotadaUrl;
    public String graficoPosicionInUrl;
    public String graficoProcesadaUrl;
    public String graficoVelocidadUrl;
    public String graficoDerivadaInUrl;
    public String graficoAceleracionInUrl;
}
