package com.gestion.qnt.controller.dto;

/**
 * Body para PUT /mi-perfil (actualizar datos editables del usuario actual).
 */
public record ActualizarMiPerfilRequest(
        String nombre,
        String apellido,
        String dni,
        /** Opcional; máx. 30 caracteres. Solo ROLE_PILOTO o ROLE_ADMIN. */
        String passwordMission,
        /** Solo ROLE_PILOTO o ROLE_ADMIN. Ignorado para otros roles. */
        Double horasVuelo,
        /** Solo ROLE_PILOTO o ROLE_ADMIN. Ignorado para otros roles. */
        Integer cantidadVuelos
) {}
