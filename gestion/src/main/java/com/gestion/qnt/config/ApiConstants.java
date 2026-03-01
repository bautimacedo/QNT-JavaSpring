package com.gestion.qnt.config;

/**
 * Constantes de URLs de la API.
 */
public final class ApiConstants {

    public static final String URL_BASE = "/api/qnt/v1";
    public static final String URL_LOGIN = URL_BASE + "/auth/login";
    public static final String URL_AUTH_ME = URL_BASE + "/auth/me";
    public static final String URL_DEMO = URL_BASE + "/demo";

    private ApiConstants() {
    }
}
