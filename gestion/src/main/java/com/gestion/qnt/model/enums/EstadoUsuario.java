package com.gestion.qnt.model.enums;

/**
 * Estado del usuario en el flujo registro → aprobación → uso.
 */
public enum EstadoUsuario {
    /** Recién registrado, pendiente de aprobación por un administrador. */
    PENDIENTE_APROBACION,

    /** Aprobado; puede usar la aplicación y hacer login. */
    ACTIVO,

    /** Desactivado por un administrador; no puede hacer login. */
    DESACTIVADO
}
