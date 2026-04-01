package com.gestion.qnt.model.enums;

public enum TipoEventoVuelo {
    DESPEGUE,
    ATERRIZAJE,
    FALLA_DESPEGUE,       // take off failure reportado por FlytBase
    DESPEGUE_FALLIDO,     // despegue + aterrizaje en menos de 1 minuto (detectado por n8n)
    MAL_TIEMPO,
    MAL_TIEMPO_MEJORADO,
    DIAGNOSTICO,
    RECORDATORIO,
    OTRO
}
