package com.gestion.qnt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FlightHubService {

    @Value("${flighthub.base-url:https://es-flight-api-us.djigate.com}")
    private String baseUrl;

    @Value("${flighthub.project-uuid:}")
    private String projectUuid;

    @Value("${flighthub.user-token:}")
    private String userToken;

    @Value("${flighthub.sn:}")
    private String sn;

    private final RestClient restClient = RestClient.builder().build();

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

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(baseUrl + "/openapi/v0.1/flight-task")
                .header("X-Request-Id",    UUID.randomUUID().toString())
                .header("X-Language",      "es")
                .header("X-Project-Uuid",  projectUuid)
                .header("X-User-Token",    userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response != null) {
            Object code = response.get("code");
            if (code instanceof Number n && n.intValue() != 0) {
                throw new RuntimeException("FlightHub error " + code + ": " + response.get("message"));
            }
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
        Map<String, Object> response = restClient.get()
                .uri(baseUrl + "/openapi/v0.1/flight-task/list"
                        + "?sn=" + sn
                        + "&begin_at=" + beginAt
                        + "&end_at=" + endAt)
                .header("X-Request-Id",   UUID.randomUUID().toString())
                .header("X-Language",     "en")
                .header("X-Project-Uuid", projectUuid)
                .header("X-User-Token",   userToken)
                .retrieve()
                .body(Map.class);

        if (response == null) return List.of();
        Object data = response.get("data");
        if (!(data instanceof Map)) return List.of();
        Object list = ((Map<?, ?>) data).get("list");
        if (!(list instanceof List)) return List.of();
        return (List<Map<String, Object>>) list;
    }
}
