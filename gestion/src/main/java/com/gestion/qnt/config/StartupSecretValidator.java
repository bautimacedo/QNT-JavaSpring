package com.gestion.qnt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Valida en startup que los secretos críticos no sean débiles ni por defecto.
 * En perfil "prod": lanza excepción y aborta. En otros perfiles: solo WARNING.
 */
@Component
public class StartupSecretValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupSecretValidator.class);

    @Value("${jwt.secret}") private String jwtSecret;
    @Value("${qnt.internal.secret}") private String internalSecret;
    @Value("${mqtt.password:}") private String mqttPassword;

    private final Environment env;

    public StartupSecretValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> problemas = new ArrayList<>();

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            problemas.add("JWT_SECRET tiene menos de 64 bytes (requerido para HMAC512)");
        }
        if (internalSecret.length() < 20) {
            problemas.add("QNT_INTERNAL_SECRET tiene menos de 20 caracteres");
        }
        if (mqttPassword.contains("prueba") || mqttPassword.contains("12345") || mqttPassword.length() < 12) {
            problemas.add("MQTT_PASSWORD es débil o corto (mín. 12 chars)");
        }

        if (!problemas.isEmpty()) {
            boolean esProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
            String msg = "⚠️  SECRETOS INSEGUROS DETECTADOS: " + String.join("; ", problemas);
            if (esProd) {
                throw new IllegalStateException(msg);
            } else {
                log.warn(msg);
            }
        }
    }
}
