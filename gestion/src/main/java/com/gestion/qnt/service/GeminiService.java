package com.gestion.qnt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
}
