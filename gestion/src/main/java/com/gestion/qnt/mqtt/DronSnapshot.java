package com.gestion.qnt.mqtt;

import java.math.BigDecimal;

public class DronSnapshot {
    public String sn;
    public BigDecimal latitud;
    public BigDecimal longitud;
    public BigDecimal altitud;
    public Boolean enDock;
    public Integer bateriaPorc;
    public BigDecimal bateriaTempC;
    public Integer bateriaCiclos;
    public String bateriaSn;
}
