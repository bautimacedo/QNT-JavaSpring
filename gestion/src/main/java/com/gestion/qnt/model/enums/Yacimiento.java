package com.gestion.qnt.model.enums;

/**
 * Yacimientos. Ojo: el NOMBRE del enum no siempre coincide con el código de "site" que se
 * guarda en vuelos_log (FlightHub registra Cañadón León como "CL", no "CANADON_LEON").
 * Para cruzar un dron con sus vuelos hay que usar {@link #getSiteCode()}, no name().
 */
public enum Yacimiento {
    CAM("CAM"),
    EFO("EFO"),
    CANADON_LEON("CL");

    private final String siteCode;

    Yacimiento(String siteCode) {
        this.siteCode = siteCode;
    }

    /** Código de site tal como aparece en vuelos_log (ej. CANADON_LEON → "CL"). */
    public String getSiteCode() {
        return siteCode;
    }
}
