package com.gestion.qnt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FlightHubService {

    @Value("${flighthub.base-url:${FLIGHTHUB_BASE_URL:https://es-flight-api-us.djigate.com}}")
    private String baseUrl;

    @Value("${flighthub.project-uuid:${FLIGHTHUB_PROJECT_UUID:}}")
    private String projectUuid;

    @Value("${flighthub.user-token:${FLIGHTHUB_USER_TOKEN:}}")
    private String userToken;

    @Value("${flighthub.sn:${FLIGHTHUB_SN:}}")
    private String sn;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutFactory())
            .build();

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(java.time.Duration.ofSeconds(10));
        f.setReadTimeout(java.time.Duration.ofSeconds(30));
        return f;
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lanza una misión inmediata en FlightHub 2.
     *
     * @param nombre      Nombre de la tarea (visible en FlightHub)
     * @param waylineUuid UUID de la wayline/ruta a ejecutar
     * @throws RuntimeException si FlightHub responde con error o code != 0
     */
    public void lanzarMision(String nombre, String waylineUuid) {
        Map<String, Object> body = Map.of(
                "name",                          nombre != null ? nombre : "Mision_QNT",
                "sn",                            sn,
                "time_zone",                     "America/Argentina/Cordoba",
                "wayline_uuid",                  waylineUuid,
                "rth_altitude",                  100,
                "rth_mode",                      "optimal",
                "wayline_precision_type",         "rtk",
                "out_of_control_action_in_flight","return_home",
                "resumable_status",              "manual",
                "task_type",                     "immediate"
        );

        try {
            String json = restClient.post()
                    .uri(baseUrl + "/openapi/v0.1/flight-task")
                    .header("X-Request-Id",    UUID.randomUUID().toString())
                    .header("X-Language",      "es")
                    .header("X-Project-Uuid",  projectUuid)
                    .header("X-User-Token",    userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (json != null) {
                Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
                Object code = response.get("code");
                if (code instanceof Number n && n.intValue() != 0) {
                    throw new RuntimeException("FlightHub error " + code + ": " + response.get("message"));
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al lanzar misión en FlightHub", e);
        }
    }

    /**
     * Consulta FlightHub para saber si el drone con el SN configurado está actualmente volando.
     * Busca tareas en ejecución en las últimas 2 horas.
     *
     * Se usa antes de lanzar una misión CAM: si FlightHub reporta una tarea activa,
     * el drone no está en el dock y el lanzamiento debe bloquearse.
     *
     * @return true si hay una tarea en ejecución, false si el drone está libre/en dock.
     *         En caso de error de comunicación con FlightHub, retorna false para no bloquear
     *         lanzamientos por problemas de red (el operador verá el error de FlightHub al lanzar).
     */
    @SuppressWarnings("unchecked")
    public boolean isDroneFlying() {
        try {
            long now     = System.currentTimeMillis() / 1000L;
            // Ventana de 24h solo para satisfacer el parámetro obligatorio de la API.
            // Lo que realmente determina si el drone está volando es flight_task_status=1
            // (en ejecución AHORA). Una misión completada aparece con status=3, no con 1.
            long dayAgo  = now - 86400;
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/task/api/v2/workspaces/" + projectUuid + "/flight-tasks")
                    .queryParam("source", 0)
                    .queryParam("sn[]", sn)
                    .queryParam("begin_at", dayAgo)
                    .queryParam("end_at", now)
                    .queryParam("page", 1)
                    .queryParam("page_size", 10)
                    .queryParam("flight_task_status", 1)  // 1 = en ejecución AHORA
                    .toUriString();

            String json = restClient.get()
                    .uri(url)
                    .header("X-User-Token",    userToken)
                    .header("X-Request-Id",    UUID.randomUUID().toString())
                    .header("X-Language-Code", "en")
                    .header("Accept",          "application/json")
                    .retrieve()
                    .body(String.class);
            if (json == null) return false;

            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            if (!(data instanceof Map)) return false;
            Object list = ((Map<?, ?>) data).get("list");
            if (!(list instanceof List)) return false;
            return !((List<?>) list).isEmpty();
        } catch (Exception e) {
            // No bloqueamos el lanzamiento por fallo de red — FlightHub rechazará si es necesario
            return false;
        }
    }

    /**
     * Lista tareas de FlightHub en un rango de tiempo.
     *
     * @param beginAt timestamp Unix en segundos
     * @param endAt   timestamp Unix en segundos
     * @return lista de tareas (cada tarea es un Map con name, uuid, status, wayline_uuid, etc.)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarTareas(long beginAt, long endAt) {
        try {
            String json = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(baseUrl + "/openapi/v0.1/flight-task/list")
                            .queryParam("sn", sn)
                            .queryParam("begin_at", beginAt)
                            .queryParam("end_at", endAt)
                            .toUriString())
                    .header("X-Request-Id",   UUID.randomUUID().toString())
                    .header("X-Language",     "en")
                    .header("X-Project-Uuid", projectUuid)
                    .header("X-User-Token",   userToken)
                    .retrieve()
                    .body(String.class);
            if (json == null) return List.of();
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            if (!(data instanceof Map)) return List.of();
            Object list = ((Map<?, ?>) data).get("list");
            if (!(list instanceof List)) return List.of();
            return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar tareas FlightHub", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerDetalleWayline(String waylineId) {
        try {
            String json = restClient.get()
                    .uri(baseUrl + "/openapi/v0.1/wayline/" + waylineId)
                    .header("X-Request-Id",   UUID.randomUUID().toString())
                    .header("X-Language",     "en")
                    .header("X-Project-Uuid", projectUuid)
                    .header("X-User-Token",   userToken)
                    .retrieve()
                    .body(String.class);
            if (json == null) return Map.of();
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            return data instanceof Map ? (Map<String, Object>) data : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener detalle de wayline " + waylineId, e);
        }
    }

    // Descarga el KMZ desde la URL firmada, descomprime y parsea las coordenadas del KML.
    // Retorna lista de {lat, lon} en orden de vuelo.
    public List<double[]> parsearCoordenadasKmz(String downloadUrl) throws Exception {
        byte[] kmzBytes = restClient.get()
                .uri(downloadUrl)
                .retrieve()
                .body(byte[].class);

        if (kmzBytes == null) return List.of();

        byte[] kmlBytes = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(kmzBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                // Zip Slip: descartar entradas con path traversal
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) continue;
                if (name.endsWith(".kml")) {
                    kmlBytes = zip.readAllBytes();
                    break;
                }
            }
        }
        if (kmlBytes == null) return List.of();

        // XXE: deshabilitar DOCTYPE y entidades externas antes de parsear
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setExpandEntityReferences(false);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(kmlBytes));

        // Busca todos los <coordinates> dentro de <Point> (waypoints individuales)
        NodeList points = doc.getElementsByTagName("Point");
        List<double[]> coords = new ArrayList<>();
        for (int i = 0; i < points.getLength(); i++) {
            NodeList children = points.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if ("coordinates".equals(children.item(j).getLocalName())
                        || "coordinates".equals(children.item(j).getNodeName())) {
                    String text = children.item(j).getTextContent().trim();
                    // KML: lon,lat,alt
                    String[] parts = text.split(",");
                    if (parts.length >= 2) {
                        double lon = Double.parseDouble(parts[0].trim());
                        double lat = Double.parseDouble(parts[1].trim());
                        coords.add(new double[]{lat, lon});
                    }
                }
            }
        }
        return coords;
    }

    /**
     * Lista tareas de FlightHub usando la API v2 (misma que usa n8n).
     * Reemplaza el workflow n8n "GET FlightHub Tasks".
     *
     * @param beginAt timestamp Unix en segundos
     * @param endAt   timestamp Unix en segundos
     */
    public List<Map<String, Object>> listarTareasV2(long beginAt, long endAt) {
        return listarTareasV2(projectUuid, sn, beginAt, endAt);
    }

    /**
     * Lista tareas de un proyecto FlightHub específico. Si {@code snFiltro} es null/vacío
     * trae todas las tareas del proyecto (útil para descubrir el SN del dock).
     */
    /** Compat: delega con flight_task_status=1 (en ejecución). */
    public List<Map<String, Object>> listarTareasV2(String projectUuid, String snFiltro, long beginAt, long endAt) {
        return listarTareasV2(projectUuid, snFiltro, beginAt, endAt, 1);
    }

    /**
     * Lista tareas de un proyecto FlightHub filtrando por {@code flightTaskStatus}
     * (1=en ejecución, 2=completada, 3/4/5=falla). Si {@code snFiltro} es null/vacío
     * trae todas las tareas del proyecto.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarTareasV2(String projectUuid, String snFiltro, long beginAt, long endAt, int flightTaskStatus) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(baseUrl + "/task/api/v2/workspaces/" + projectUuid + "/flight-tasks")
                    .queryParam("source", 0)
                    .queryParam("begin_at", beginAt)
                    .queryParam("end_at", endAt)
                    .queryParam("page", 1)
                    .queryParam("page_size", 50)
                    .queryParam("flight_task_status", flightTaskStatus)
                    .queryParam("list_type", 0); // paridad con el workflow n8n original
            if (snFiltro != null && !snFiltro.isBlank()) {
                builder.queryParam("sn[]", snFiltro);
            }
            String url = builder.toUriString();

            String json = restClient.get()
                    .uri(url)
                    .header("X-User-Token",    userToken)
                    .header("X-Request-Id",    UUID.randomUUID().toString())
                    .header("X-Language-Code", "en")
                    .header("Accept",          "application/json")
                    .retrieve()
                    .body(String.class);
            if (json == null) return List.of();
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            if (!(data instanceof Map)) return List.of();
            Object list = ((Map<?, ?>) data).get("list");
            if (!(list instanceof List)) return List.of();
            return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar tareas FlightHub v2", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarWaylines() {
        try {
            String json = restClient.get()
                    .uri(baseUrl + "/openapi/v0.1/wayline")
                    .header("X-Request-Id",   UUID.randomUUID().toString())
                    .header("X-Language",     "en")
                    .header("X-Project-Uuid", projectUuid)
                    .header("X-User-Token",   userToken)
                    .retrieve()
                    .body(String.class);
            if (json == null) return List.of();
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            if (!(data instanceof Map)) return List.of();
            Object list = ((Map<?, ?>) data).get("list");
            if (!(list instanceof List)) return List.of();
            return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar waylines FlightHub", e);
        }
    }
}
