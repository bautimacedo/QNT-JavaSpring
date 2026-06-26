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
            CONTEXTO DEL PROYECTO:
            QNT Drones es un sistema de gestión para una operación de drones (DJI Matrice 4TD + Dock 3,
            plataforma Flytbase) dedicada a la inspección de pozos en la industria oil & gas. Los drones
            operan de forma autónoma desde docks en tres yacimientos (Cañadón Amarillo, Estación Fernández
            Oro y Cañadón León). El sistema mantiene la trazabilidad de todo lo relacionado a los drones:
            vuelos, misiones, clima, stock, mantenimiento e incidentes. Quienes cargan estas horas son los
            desarrolladores y operadores que construyen y mantienen este sistema de software.

            TU TAREA:
            Sos un asistente que mejora la redacción de la descripción de un PARTE DE HORAS de trabajo.
            Tomá el texto breve de abajo y reformulalo de forma clara y profesional en español rioplatense,
            manteniéndote FIEL a lo que escribió la persona.

            REGLAS ESTRICTAS:
            - Conservador: mejorá la redacción, NO inventes tareas, actividades, áreas ni contexto que no
              estén en el texto original.
            - Proporcional al input: si el texto es corto, devolvé 1 a 3 oraciones. NUNCA generes informes
              largos ni varios párrafos a partir de pocas palabras.
            - NO inventes datos específicos (cantidades, nombres, horarios, herramientas).
            - Redactá usando sustantivos/infinitivos (ej. "Validación del módulo de horas", "Testeo de
              misiones en CAM"), NUNCA en primera persona (nada de "validé", "hice", "estuve").
            - Usá el contexto del proyecto SOLO para entender el vocabulario, NO para agregar tareas.
            - Devolvé SOLO el texto redactado, sin títulos, encabezados ni comillas.

            Texto a mejorar: %s
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
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
            A partir del siguiente texto en lenguaje natural, extraé los registros de trabajo (uno por tarea/día).
            Reglas:
            - Resolvé fechas relativas a fechas absolutas YYYY-MM-DD. "los últimos N días" = un registro por
              cada uno de esos días (sin contar futuro). "ayer", "el lunes", etc. también a fecha absoluta.
            - "horas": número decimal. Si el texto NO especifica las horas, ESTIMÁ una cantidad razonable de
              jornada (entre 2 y 6) — el usuario las podrá ajustar después.
            - NO inventes TAREAS que no estén mencionadas en el texto (las horas sí podés estimarlas).
            - "descripcion": redactá la tarea de forma profesional y concisa usando sustantivos/infinitivos
              (ej. "Testeo de misiones en CAM", "Validación del módulo de horas"), NUNCA en primera persona
              (nada de "hice", "validé", "estuve").
            - Si el texto describe trabajo pero es vago, generá igual al menos un registro (no devuelvas vacío).

            Texto: %s
            """;

    private static final String PROMPT_GENERAR = """
            CONTEXTO DEL PROYECTO:
            QNT Drones es un sistema de gestión para una operación de drones (DJI Matrice 4TD + Dock 3,
            plataforma Flytbase) dedicada a la inspección de pozos en oil & gas, con drones autónomos en
            tres yacimientos (Cañadón Amarillo, Estación Fernández Oro y Cañadón León). El sistema (software)
            tiene módulos reales: partes de horas, tickets de soporte, clima (estación Tempest), registro de
            vuelos (FlightHub/Flytbase), stock, mantenimiento, alertas, panel ejecutivo y un bot de Telegram.

            TU TAREA:
            Hoy es %s (zona ART). A partir de la indicación de la persona, GENERÁ una lista de registros de
            trabajo PLAUSIBLES que un desarrollador/operador de este proyecto podría haber hecho. Inventá
            tareas creíbles y variadas dentro del dominio (desarrollo y fixes de módulos, deploys, pruebas,
            análisis de logs, configuración de drones/docks, soporte, reuniones técnicas, etc.).

            REGLAS:
            - Interpretá el PERÍODO que indica la persona y RESPETALO de forma ESTRICTA:
              * Si menciona un solo día ("hoy", "ayer", una fecha puntual) → TODOS los registros van en ESE
                mismo día (podés poner varias tareas distintas el mismo día, no inventes otros días).
              * Si menciona un rango ("esta semana", "los últimos 5 días", "del 10 al 15") → distribuí las
                fechas dentro de ese rango.
              * Si NO menciona ningún período → usá únicamente el día de hoy.
              * NUNCA uses fechas fuera del período indicado, ni fechas futuras (posteriores a hoy).
            - Cantidad de registros: para un solo día, 1 a 3 tareas; para un rango, 1 o 2 por día (sin pasar
              de ~6 en total). No infles la cantidad si el período es chico.
            - Horas realistas por registro: entre 1 y 8.
            - Descripciones concretas y profesionales, variadas entre sí, redactadas con sustantivos/infinitivos
              (ej. "Desarrollo del módulo de tickets", "Análisis de logs del Dock"), NUNCA en primera persona.
            - Devolvé SOLO el array JSON.

            Indicación de la persona: %s
            """;

    /**
     * Parsea texto libre en borradores FIELES a lo que escribió la persona (no inventa).
     * Lista vacía si falta key o falla.
     */
    public List<BorradorHora> parsearRegistros(String texto, LocalDate hoy) {
        if (texto == null || texto.isBlank()) return new ArrayList<>();
        return ejecutarGeneracionJson(String.format(PROMPT_PARSEO, hoy, texto));
    }

    /**
     * "Asistente +": GENERA borradores plausibles (inventados pero creíbles del dominio)
     * a partir de una indicación breve. Lista vacía si falta key o falla.
     */
    public List<BorradorHora> generarRegistros(String indicacion, LocalDate hoy) {
        if (indicacion == null || indicacion.isBlank()) return new ArrayList<>();
        return ejecutarGeneracionJson(String.format(PROMPT_GENERAR, hoy, indicacion));
    }

    /** Llama a Gemini con salida estructurada (array de {fecha,horas,descripcion}) y parsea el resultado. */
    private List<BorradorHora> ejecutarGeneracionJson(String promptText) {
        List<BorradorHora> out = new ArrayList<>();
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
                    Map.of("parts", new Object[]{ Map.of("text", promptText) })
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
            log.error("[Gemini] error generando registros: {}", e.getMessage());
            return out;
        }
    }
}
