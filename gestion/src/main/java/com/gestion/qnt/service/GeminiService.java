package com.gestion.qnt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.qnt.controller.dto.BorradorHora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String PROMPT = """
            Sos un asistente que redacta partes de trabajo técnicos para una empresa de drones.
            A partir de la descripción breve de abajo, redactá una versión más completa y profesional
            en español rioplatense, en uno o dos párrafos. Detallá las tareas de forma clara y formal,
            pero NO inventes datos específicos (cantidades, nombres, horarios, herramientas) que no
            estén en el texto original. Devolvé SOLO el texto redactado, sin títulos ni comillas.

            Descripción breve: %s
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.apiKey = apiKey;
        this.model  = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Amplía una descripción breve usando Gemini. Si no hay API key o falla la llamada,
     * devuelve el texto original (degradación elegante, no rompe el flujo de carga).
     */
    public String ampliarDescripcion(String textoBreve) {
        if (textoBreve == null || textoBreve.isBlank()) return textoBreve;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Gemini] GEMINI_API_KEY no configurada, devolviendo texto original");
            return textoBreve;
        }
        try {
            Map<String, Object> body = Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{
                        Map.of("text", String.format(PROMPT, textoBreve))
                    })
                }
            );
            String json = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            JsonNode texto = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            if (texto.isMissingNode() || texto.asText().isBlank()) {
                log.warn("[Gemini] respuesta sin texto, devolviendo original");
                return textoBreve;
            }
            return texto.asText().trim();
        } catch (Exception e) {
            log.error("[Gemini] error ampliando descripción: {}", e.getMessage());
            return textoBreve;
        }
    }

    private static final String PROMPT_PARSEO = """
            Hoy es %s (zona horaria America/Argentina/Buenos_Aires).
            A partir del siguiente texto en lenguaje natural, extraé los registros de trabajo.
            Reglas:
            - Resolvé fechas relativas (ayer, hoy, el lunes, anteayer) a fecha absoluta en formato YYYY-MM-DD.
            - "horas" es un número decimal de horas trabajadas (ej. 2.5).
            - "descripcion" es la tarea redactada de forma clara y profesional en español rioplatense.
            - NO inventes horas, fechas ni tareas que no estén implícitas en el texto.
            - Si el texto menciona varios días o tareas, devolvé un registro por cada uno.

            Texto: %s
            """;

    /**
     * Parsea texto libre en una lista de borradores de registro de horas usando salida
     * estructurada (JSON) de Gemini. Si falta la key o falla, devuelve lista vacía.
     */
    public List<BorradorHora> parsearRegistros(String texto, LocalDate hoy) {
        List<BorradorHora> out = new ArrayList<>();
        if (texto == null || texto.isBlank()) return out;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Gemini] GEMINI_API_KEY no configurada, asistente sin resultados");
            return out;
        }
        try {
            Map<String, Object> itemSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                    "fecha",       Map.of("type", "STRING", "description", "YYYY-MM-DD"),
                    "horas",       Map.of("type", "NUMBER"),
                    "descripcion", Map.of("type", "STRING")
                ),
                "required", new String[]{"fecha", "horas", "descripcion"}
            );
            Map<String, Object> body = Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{
                        Map.of("text", String.format(PROMPT_PARSEO, hoy, texto))
                    })
                },
                "generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "responseSchema", Map.of("type", "ARRAY", "items", itemSchema)
                )
            );
            String json = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            JsonNode textNode = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode()) return out;

            JsonNode arr = objectMapper.readTree(textNode.asText());
            if (!arr.isArray()) return out;
            for (JsonNode n : arr) {
                try {
                    LocalDate fecha = LocalDate.parse(n.path("fecha").asText());
                    BigDecimal horas = new BigDecimal(n.path("horas").asText());
                    String desc = n.path("descripcion").asText("");
                    out.add(new BorradorHora(fecha, horas, desc));
                } catch (Exception ignore) { /* fila mal formada: la salteamos */ }
            }
            return out;
        } catch (Exception e) {
            log.error("[Gemini] error parseando registros: {}", e.getMessage());
            return out;
        }
    }
}
