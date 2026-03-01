package com.gestion.qnt.controller.dto;

public record ChangePasswordRequest(
        String email,
        String oldPassword,
        String newPassword
) {}
