package com.gestion.qnt.debug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Instrumentación de depuración: escribe una línea NDJSON al archivo de log.
 * No loguear secretos (passwords, tokens).
 */
public final class DebugLog {

    private static final Path LOG_PATH = Path.of(
            "/home/bauti/Proyectos GITHUB/QNT-Gestion-Spring/.cursor/debug.log");

    public static void log(String location, String message, Map<String, Object> data, String hypothesisId) {
        try {
            long ts = System.currentTimeMillis();
            String json = String.format(
                    "{\"timestamp\":%d,\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"hypothesisId\":\"%s\"}",
                    ts, escape(location), escape(message), mapToJson(data), hypothesisId != null ? hypothesisId : "");
            Files.writeString(LOG_PATH, json + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private static String mapToJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        data.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(escape(k)).append("\":");
            if (v == null) sb.append("null");
            else if (v instanceof Boolean) sb.append(v);
            else if (v instanceof Number) sb.append(v);
            else sb.append("\"").append(escape(String.valueOf(v))).append("\"");
        });
        return sb.append("}").toString();
    }
}
