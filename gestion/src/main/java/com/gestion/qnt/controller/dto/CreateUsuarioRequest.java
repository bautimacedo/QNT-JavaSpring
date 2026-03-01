package com.gestion.qnt.controller.dto;

import java.util.List;

public record CreateUsuarioRequest(
        String nombre,
        String apellido,
        String email,
        String password,
        List<Long> roleIds,
        List<String> roleCodigos
) {}
