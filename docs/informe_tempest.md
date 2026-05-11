# Informe de Integración — Estación Meteorológica Tempest (Cañadón León)

## Contexto

El jefe de operaciones adquirió una estación **WeatherFlow Tempest** para instalar en el sitio Cañadón León. El objetivo es monitorear las condiciones climáticas en tiempo real —especialmente el viento— para determinar si es seguro operar drones en esa zona. Este documento detalla la API disponible, los datos que se pueden obtener y las ideas de integración con el sistema QNT.

---

## 1. La API de Tempest

### Autenticación

Existen dos métodos:

**A. Personal Access Token (recomendado para QNT)**
- Obtenerlo en: https://tempestwx.com → Settings → Data Authorizations → "Create Token"
- Se pasa como query param en cada request: `?token=TU_TOKEN`
- Sin expiración documentada. Ideal para un servicio backend de uso interno.

**B. OAuth 2.0**
- Para aplicaciones multi-usuario. No aplica a este caso.

### URLs base

| Canal | URL |
|-------|-----|
| REST API | `https://swd.weatherflow.com/swd/rest` |
| WebSocket | `wss://ws.weatherflow.com/swd/data?token=TOKEN` |
| UDP local (fallback sin internet) | Puerto `50222` en la red local del Hub |

---

## 2. Cómo obtener Station ID y Device ID

Llamar una sola vez al endpoint de estaciones con el token configurado:

```
GET https://swd.weatherflow.com/swd/rest/stations?token=TU_TOKEN
```

La respuesta incluye para cada estación:
- `station_id` (int) — identificador de la estación
- `device_id` (int) — identificador del dispositivo Tempest (tipo `"ST"`)
- `serial_number` (string, ej: `"ST-12345678"`)
- `latitude`, `longitude`, `elevation`
- `timezone`

Guardar estos IDs en `application.properties` y nunca más hace falta llamar a este endpoint.

---

## 3. Endpoints disponibles

### 3.1 Observación actual — campos nombrados (el más útil)

```
GET /observations/station/{station_id}?token=TOKEN
```

Devuelve la última observación con **campos con nombre** (no arrays). Un solo llamado da todo lo necesario para el semáforo de vuelo.

**Campos de la respuesta (dentro de `obs[0]`):**

| Campo | Unidad | Descripción |
|-------|--------|-------------|
| `timestamp` | epoch UTC | Momento de la observación |
| `wind_avg` | m/s | Velocidad promedio de viento |
| `wind_gust` | m/s | Ráfaga máxima |
| `wind_lull` | m/s | Viento mínimo en el intervalo |
| `wind_direction` | grados (0-360) | Dirección del viento |
| `air_temperature` | °C | Temperatura del aire |
| `relative_humidity` | % | Humedad relativa |
| `barometric_pressure` | mb | Presión barométrica en estación |
| `sea_level_pressure` | mb | Presión ajustada al nivel del mar |
| `precip` | mm | Precipitación en el último minuto |
| `precip_accum_last_1hr` | mm | Lluvia acumulada última hora |
| `uv` | índice | Índice UV |
| `solar_radiation` | W/m² | Radiación solar |
| `brightness` | lux | Iluminancia |
| `lightning_strike_count_last_3hr` | count | Rayos en las últimas 3 horas |
| `lightning_strike_last_distance` | km | Distancia del último rayo |
| `lightning_strike_last_epoch` | epoch UTC | Momento del último rayo |
| `air_density` | kg/m³ | Densidad del aire (afecta empuje de motores) |
| `feels_like` | °C | Sensación térmica |
| `dew_point` | °C | Punto de rocío |
| `delta_t` | °C | Diferencia entre temperatura y bulbo húmedo |

---

### 3.2 Historial de observaciones (resolución 1 minuto)

```
GET /observations/device/{device_id}?token=TOKEN&day_offset=0     # hoy UTC
GET /observations/device/{device_id}?token=TOKEN&day_offset=1     # ayer UTC
GET /observations/device/{device_id}?token=TOKEN&time_start=X&time_end=Y  # rango de hasta 5 días
GET /observations/device/{device_id}?token=TOKEN&format=csv       # en formato CSV
```

Los datos vienen como arrays de 22 valores por observación (Tempest `obs_st`):

```
[0]  Epoch (s UTC)
[1]  Wind Lull (m/s)
[2]  Wind Avg (m/s)
[3]  Wind Gust (m/s)
[4]  Wind Direction (grados)
[5]  Wind Sample Interval (s)
[6]  Pressure (mb)
[7]  Air Temperature (°C)
[8]  Relative Humidity (%)
[9]  Illuminance (lux)
[10] UV (índice)
[11] Solar Radiation (W/m²)
[12] Rain Accumulation (mm)
[13] Precipitation Type (0=ninguna, 1=lluvia, 2=granizo, 3=lluvia+granizo)
[14] Lightning Avg Distance (km)
[15] Lightning Strike Count
[16] Battery (V)
[17] Report Interval (min)
[18] Local Day Rain Accumulation (mm)
[19] NC Rain Accumulation (mm) — RainCheck corregido
[20] Local Day NC Rain Accumulation (mm)
[21] Precipitation Analysis Type
```

---

### 3.3 Pronóstico (forecast)

```
GET /better_forecast?station_id={station_id}&token=TOKEN&units_wind=mps
```

Parámetros de unidades opcionales: `units_wind=mps`, `units_temp=c`, `units_pressure=mb`, `units_precip=mm`, `units_distance=km`.

**Respuesta:**

`current_conditions`:
- Igual que `/observations/station/` pero con `pressure_trend` (rising/steady/falling), `wind_direction_cardinal` (N, NNE, NE, etc.)

`forecast.hourly[]` — pronóstico hora por hora:
- `time`, `air_temperature`, `wind_avg`, `wind_gust`, `wind_direction`, `wind_direction_cardinal`
- `relative_humidity`, `precip_probability` (%), `conditions` (string), `uv`

`forecast.daily[]` — pronóstico diario:
- `air_temp_high`, `air_temp_low`, `precip_probability`, `conditions`, `sunrise`, `sunset`

**Strings de condición posibles:**
`Clear`, `Rain Likely`, `Rain Possible`, `Snow`, `Thunderstorms Likely`, `Thunderstorms Possible`, `Windy`, `Foggy`, `Cloudy`, `Partly Cloudy`, `Very Light Rain`

---

## 4. WebSocket — Datos en tiempo real

```
wss://ws.weatherflow.com/swd/data?token=TOKEN
```

- Una sola conexión por cliente
- Se desconecta automáticamente tras **10 minutos de inactividad** → implementar reconexión automática

**Mensajes de suscripción (enviar al conectar):**

```json
// Viento cada 3 segundos
{"type": "listen_rapid_start", "device_id": 12345, "id": "uid-1"}

// Observación completa cada ~1 minuto
{"type": "listen_start", "device_id": 12345, "id": "uid-2"}
```

**Mensajes que llegan:**

| Tipo | Frecuencia | Contenido |
|------|-----------|-----------|
| `rapid_wind` | Cada 3 s | `"ob": [epoch, wind_speed_m/s, wind_dir_deg]` |
| `obs_st` | Cada ~1 min | Array de 22 campos (igual que REST device) |
| `evt_precip` | Al inicio de lluvia | `{type, device_id}` |
| `evt_strike` | Inmediato al rayo | `"evt": [epoch, distancia_km, energía]` |
| `ack` | Respuesta a cada send | Confirmación de suscripción |

---

## 5. Rate Limits

No se publican límites numéricos explícitos en la documentación. La política establece:
- Cuentas personales: límites "bajos"
- Cuentas comerciales (WeatherFlowONE): límites "altos"

**Práctica segura:** no hacer REST polling más de una vez por minuto (el dispositivo actualiza cada 60 s de todas formas). Para datos en tiempo real usar WebSocket en lugar de polling frecuente.

---

## 6. Fallback UDP Local

Si el sitio Cañadón León tiene conectividad a internet inestable, el Hub de Tempest emite los mismos datos por **UDP broadcast en la red local**, puerto `50222`. No requiere internet, solo que el servidor QNT esté en la misma LAN que el Hub.

Los tipos de mensaje son idénticos al WebSocket: `rapid_wind`, `obs_st`, `evt_precip`, `evt_strike`, `device_status`, `hub_status`.

Recomendado como **backup** automático si la conexión WebSocket falla.

---

## 7. Ideas de integración con QNT

### 7.1 Semáforo de vuelo en el Dashboard

Un widget visible en el panel principal indicando si Cañadón León es operable en este momento:

| Estado | Condición |
|--------|-----------|
| 🟢 **APTO** | Viento < 10 m/s, ráfaga < 13 m/s, sin lluvia, sin rayos en < 10 km |
| 🟡 **PRECAUCIÓN** | Viento entre 10-12 m/s, o condición marginal |
| 🔴 **NO VOLAR** | Viento > 12 m/s, ráfaga > 15 m/s, lluvia activa, o rayo < 10 km |

Los umbrales deben ser configurables por parámetro de ambiente, ya que varían según el modelo de dron y la política interna de la empresa.

---

### 7.2 Bloqueo preventivo al lanzar misiones en Cañadón León

Antes de confirmar el lanzamiento de una misión cuyo dron es de Cañadón León, el backend consulta Tempest en tiempo real. Si las condiciones superan los límites, responde `409 CONFLICT`:

> "Viento actual: 18.3 m/s (ráfaga: 23.1 m/s). Supera el límite operacional de 12 m/s para Cañadón León. Misión bloqueada."

El ADMIN puede hacer **override explícito** con justificación — queda registrado en el log de auditoría con timestamp y usuario.

---

### 7.3 Snapshot meteorológico en cada VueloLog

Al registrar un vuelo, guardar las condiciones del momento del despegue:
- `viento_avg_ms`, `viento_gust_ms`, `viento_dir`, `temperatura`, `humedad`, `presion`, `lluvia_activa`, `rayos_3h`

Utilidad:
- **Análisis post-incidente**: correlacionar fallas con condiciones climáticas
- **Reportes operacionales** y eventualmente a ANAC
- **Estadísticas**: en qué condiciones se vuela más y con qué resultados

---

### 7.4 Alertas automáticas al bot de Telegram

Un job schedulado cada 5 minutos evalúa las condiciones de Cañadón León. Si se cruza un umbral (por ejemplo, viento sube de apto a no volar), el bot envía al canal de operaciones:

```
⚠️ ALERTA METEOROLÓGICA — Cañadón León
Viento: 21.4 m/s · Ráfaga: 26.8 m/s · Dirección: NO
Rayo detectado a 7 km (hace 4 min)
🔴 Condiciones NO aptas para volar
Última observación: 14:37 hs (hace 2 min)
```

Solo se envía alerta al **cambiar de estado** (no spam en cada ciclo si ya se sabe que está en rojo).

---

### 7.5 Forecast pre-turno en el Calendario de Misiones

Al abrir el Calendario de Misiones, mostrar una barra de pronóstico horario del día para Cañadón León: los pilotos ven qué ventana horaria es viable antes de llegar al campo. Cada hora muestra viento promedio, ráfaga y un ícono de condición.

---

### 7.6 Historial de clima en la vista de Cañadón León

Una sección con las observaciones de los últimos 7 días como gráfico de línea: viento promedio y ráfaga vs. tiempo. Útil para detectar patrones (ej: en esa zona suele ventear fuerte después de las 14 hs).

---

## 8. Arquitectura técnica propuesta (Spring Boot + Vue)

### Backend

```
TempestService.java
  ├── getObservacionActual()      → GET /observations/station/{id}   (resultado cacheado)
  ├── getForecastHorario()        → GET /better_forecast              (cache 30 min)
  ├── esAptoParaVolar(yacimiento) → lógica de semáforo según umbrales configurados
  └── (futuro) WebSocketClient    → viento cada 3s + eventos de rayo

TempestScheduler.java
  ├── @Scheduled(60s)   → actualiza cache de observación actual
  ├── @Scheduled(5min)  → evalúa alertas y notifica Telegram si cambia estado
  └── @Scheduled(30min) → actualiza cache de forecast

TempestController.java
  └── GET /api/qnt/v1/meteo/{yacimiento}  → condiciones actuales + semáforo al frontend
```

### Variables de entorno / application.properties

```properties
tempest.token=TU_TOKEN_AQUI
tempest.caniadon.station-id=XXXXX
tempest.caniadon.device-id=YYYYY
tempest.umbrales.viento-max-ms=12.0
tempest.umbrales.rafaga-max-ms=15.0
tempest.umbrales.rayo-min-km=10.0
tempest.umbrales.viento-precaucion-ms=10.0
```

### Tabla DB opcional — historial meteorológico

```sql
CREATE TABLE meteo_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    yacimiento        VARCHAR(50)   NOT NULL,      -- 'CANIADON_LEON', 'CAM', etc.
    timestamp         TIMESTAMPTZ   NOT NULL,
    viento_avg_ms     DECIMAL(5,2),
    viento_gust_ms    DECIMAL(5,2),
    viento_dir_grados INTEGER,
    temperatura_c     DECIMAL(5,2),
    humedad_pct       DECIMAL(5,2),
    presion_mb        DECIMAL(8,2),
    lluvia_mm_min     DECIMAL(6,2),
    rayos_3h          INTEGER,
    rayo_dist_km      DECIMAL(6,2),
    uv_index          DECIMAL(4,2),
    densidad_aire     DECIMAL(6,4),
    condicion         VARCHAR(20),                 -- APTO / PRECAUCION / NO_VOLAR
    raw_json          TEXT                         -- respuesta completa de la API
);

CREATE INDEX ON meteo_snapshots (yacimiento, timestamp DESC);
```

### Frontend

```
MeteoWidget.vue          → badge compacto en header: 🟢 Cañadón León (8.2 m/s)
MeteoCaniadonView.vue    → página completa: condiciones actuales + forecast + historial
```

---

## 9. Pasos para arrancar (MVP)

1. **Crear token personal** en https://tempestwx.com/settings → Data Authorizations
2. **Encender la estación** y conectarla al Hub en Cañadón León
3. Llamar `GET /stations?token=TU_TOKEN` para obtener `station_id` y `device_id`
4. Configurar las variables en `application-prod.properties`
5. Implementar `TempestService` con polling REST cada 60 s y cache en memoria
6. Agregar endpoint `GET /meteo/caniadon` en `TempestController`
7. Agregar `MeteoWidget.vue` en el dashboard
8. Implementar bloqueo en el endpoint de lanzar misiones de Cañadón León
9. **Segunda iteración:** alertas Telegram + snapshot en VueloLog + historial en DB
10. **Tercera iteración:** WebSocket para datos de viento cada 3 segundos + forecast en Calendario

---

## 10. Recursos

- Documentación oficial: https://weatherflow.github.io/Tempest/api/
- Swagger/OpenAPI spec: https://weatherflow.github.io/Tempest/api/swagger/swagger.json
- Portal de usuario (token + station ID): https://tempestwx.com
- WebSocket reference: https://weatherflow.github.io/Tempest/api/ws.html
- UDP local reference: https://weatherflow.github.io/Tempest/api/udp.html
