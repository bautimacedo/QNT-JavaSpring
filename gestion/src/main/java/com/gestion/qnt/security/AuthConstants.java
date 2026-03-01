package com.gestion.qnt.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constantes para autenticación JWT (header, param, prefijo).
 * Secret y expiración se leen de configuración.
 */
@Component
public class AuthConstants {

    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String AUTH_PARAM_NAME = "authtoken";
    public static final String TOKEN_PREFIX = "Bearer ";

    @Value("${jwt.secret:MyVerySecretKeyChangeInProduction}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
