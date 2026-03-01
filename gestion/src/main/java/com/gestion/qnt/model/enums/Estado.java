package com.gestion.qnt.model.enums;

/**
 * Estado de equipos: Dock, Dron, Bateria, Helice, AntenaRtk, AntenaStarlink.
 */
public enum Estado {
    /** En oficina/almacén, disponible para enviar */
    STOCK_ACTUAL,
    /** En camino al site o en reparación/servicio */
    EN_PROCESO,
    /** Desplegado y en uso en el site */
    STOCK_ACTIVO,
    /** Retirado definitivamente (baja) */
    EN_DESUSO
}
