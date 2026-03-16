package com.gestion.qnt.mqtt;

import java.math.BigDecimal;

public class DockSnapshot {
    public String sn;
    public BigDecimal latitud;
    public BigDecimal longitud;
    public BigDecimal altitud;
    public BigDecimal temperaturaAmbiente;
    public BigDecimal velocidadViento;

    // Drone asociado al dock
    public String droneSn;
    public Boolean droneEnDock;
    public Integer droneBateriaPorc;
    public BigDecimal droneBateriaTempC;
    public Integer droneBateriaCiclos;
}
