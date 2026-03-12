# Informe Backend para Frontend — QNT Gestión

**Proyecto:** QNT-Gestion-Spring (backend)
**Destinatario:** Equipo/Agentes de frontend
**Objetivo:** Mantener alineación entre backend y frontend: contratos de API, modelos de datos, autenticación y convenciones.

---

## 1. Resumen ejecutivo

Este documento describe el **contrato del backend** para que el proyecto frontend pueda consumir la API REST de forma coherente. Incluye: base URL, autenticación JWT, listado de recursos y endpoints, formas de los request/response, códigos HTTP, roles, enums y recomendaciones de uso.

**Stack backend:** Spring Boot 3.x, Java 17+, Spring Security (JWT), JPA/Hibernate, PostgreSQL.

**Última actualización:** v3.0 — Incorporación de Misiones, Tareas, Alertas, Logs, Mantenimiento (Drones/Docks), Clima (OpenWeatherMap), Mapa de equipos. Site ahora tiene campo `codigo`. Tabla `clima_registros` con lógica `is_flyable`.

---

## 2. Base URL y entorno

| Concepto | Valor |
|----------|--------|
| **Prefijo de la API** | `/api/qnt/v1` |
| **Puerto por defecto** | `8081` (producción/docker: `8080`) |
| **URL base completa (local)** | `http://localhost:8081/api/qnt/v1` |

> **Nota:** En desarrollo local el backend corre en el puerto **8081** (`SERVER_PORT=8081`) porque el 8080 y 8085 están ocupados por Evolution API (WhatsApp). En producción/Docker usa el 8080 por defecto.

El frontend Vite tiene configurado un proxy: `/api/qnt/v1` → `http://localhost:8081`.

---

## 3. Autenticación (JWT)

### 3.1 Rutas públicas (sin token)

- **POST** `/api/qnt/v1/auth/login`
- **POST** `/api/qnt/v1/auth/register`
- **GET** `/api/qnt/v1/clima` — Datos meteorológicos (público, usado en landing)
- **GET** `/api/qnt/v1/clima/{codigo}` — Datos de un site específico (público)
- **GET** `/api/qnt/v1/clima/{codigo}/historial` — Historial de clima (público)
- **GET** `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**`

### 3.2 Cómo enviar el token

- **Header:** `Authorization: Bearer <token>`
- **Query (alternativo):** `?authtoken=<token>`

Token expira en **1 hora** (3 600 000 ms). No hay endpoint de refresh; hacer login de nuevo.

### 3.3 Login

- **URL:** `POST /api/qnt/v1/auth/login`
- **Body JSON:** `{ "username": "email@ejemplo.com", "password": "contraseña" }`
- **Respuesta éxito (200):** Cuerpo en **texto plano** con el JWT (no es JSON).
- **Errores:** 400 falta params / 401 credenciales incorrectas / 403 cuenta pendiente o desactivada / 500 error interno.

### 3.4 Registro

- **URL:** `POST /api/qnt/v1/auth/register`
- **Body:** `{ "nombre", "apellido"?, "email", "password" }`
- **201:** Usuario creado en estado `PENDIENTE_APROBACION`. No puede hacer login hasta que ADMIN lo apruebe.

### 3.5 Usuario actual

- **GET** `/api/qnt/v1/auth/me` → `{ id, email, username, authorities: ["ROLE_ADMIN"] }`

---

## 4. CORS

Orígenes permitidos configurados en `app.cors.allowed-origins`: `https://qnt.dronefieldoperation.cloud, http://localhost:5173`. Métodos: GET, POST, PUT, DELETE, OPTIONS, PATCH.

---

## 5. Recursos y endpoints

### 5.1 Auth

| Método | Ruta | Auth |
|--------|------|------|
| POST | `/auth/login` | No |
| POST | `/auth/register` | No |
| GET | `/auth/me` | Sí |

### 5.2 Usuarios

Base: `/usuarios`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/usuarios` | ADMIN |
| GET | `/usuarios/pendientes` | ADMIN |
| GET | `/usuarios/pilotos` | ADMIN |
| GET | `/usuarios/search?email=` | ADMIN |
| POST | `/usuarios` | ADMIN |
| PUT | `/usuarios/{id}` | ADMIN |
| PUT | `/usuarios/{id}/aprobar` | ADMIN |
| POST | `/usuarios/change-password` | ADMIN, USER |
| PUT | `/usuarios/disable?email=` | ADMIN |
| PUT | `/usuarios/enable?email=` | ADMIN |
| PUT | `/usuarios/assign-role` | ADMIN |
| PUT | `/usuarios/remove-role` | ADMIN |

### 5.3 Roles

Base: `/roles`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/roles` | ADMIN |
| GET | `/roles/search?codigo=` | ADMIN |
| POST | `/roles` | ADMIN |
| PUT | `/roles/{id}` | ADMIN |

### 5.4 Compras

Base: `/compras`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/compras` | Autenticado |
| GET | `/compras/{id}` | ADMIN, USER |
| POST | `/compras` | ADMIN, USER |
| PUT | `/compras/{id}` | ADMIN, USER |
| DELETE | `/compras/{id}` | ADMIN |
| PUT | `/compras/{id}/imagen` | ADMIN, USER |
| GET | `/compras/{id}/imagen` | ADMIN, USER |
| GET | `/compras/tipos-equipo` | Autenticado |

### 5.4.1 Proveedores

Base: `/proveedores`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/proveedores` | Autenticado |
| GET | `/proveedores/search?nombre=` | Autenticado |
| GET | `/proveedores/{id}` | Autenticado |
| POST | `/proveedores` | Autenticado |
| PUT | `/proveedores/{id}` | Autenticado |
| DELETE | `/proveedores/{id}` | ADMIN |

### 5.4.2 Empresas

Base: `/empresas`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/empresas` | ADMIN |
| GET | `/empresas/{id}` | ADMIN |
| POST | `/empresas` | ADMIN |
| PUT | `/empresas` | ADMIN |
| DELETE | `/empresas/{id}` | ADMIN |
| POST | `/empresas/{id}/sites` | ADMIN |

### 5.4.3–5.4.9 Equipos de stock

| Recurso | Base URL | Auth |
|---------|----------|------|
| Drones | `/drones` | Autenticado |
| Docks | `/docks` | Autenticado |
| Baterías | `/baterias` | Autenticado |
| Hélices | `/helices` | Autenticado |
| Antenas RTK | `/antenas-rtk` | Autenticado |
| Antenas Starlink | `/antenas-starlink` | Autenticado |
| Accesorios | `/accesorios` | Autenticado |

Todos exponen GET (lista), GET /{id}, POST, PUT, DELETE. El **PUT** no lleva `{id}` en la URL; el id va en el body.

> **Nota `Dock`:** El campo `site` (ManyToOne) tiene `@JsonIgnore` — no se serializa en la respuesta GET /docks. Para conocer el site de un dock usar el contexto de la aplicación o el campo `siteId` al crear/actualizar.

### 5.5 Licencias (SW)

Base: `/licencias` — GET, GET/{id}, POST, PUT/{id}, DELETE/{id} (DELETE solo ADMIN).

### 5.6 Seguros

Base: `/seguros` — GET, GET/{id}, POST, PUT/{id}, DELETE/{id} (DELETE solo ADMIN).

### 5.7 Mi perfil

Base: `/mi-perfil`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/mi-perfil` | Autenticado |
| PUT | `/mi-perfil` | Autenticado |
| PUT | `/mi-perfil/cambio-password` | Autenticado |
| PUT | `/mi-perfil/foto-perfil` | Autenticado |
| GET | `/mi-perfil/foto-perfil` | Autenticado |
| GET/POST/PUT/DELETE | `/mi-perfil/licencias[/{id}]` | PILOTO, ADMIN |
| PUT/GET | `/mi-perfil/licencias/{id}/imagen-cma` | PILOTO, ADMIN |
| PUT/GET | `/mi-perfil/licencias/{id}/imagen-certificado-idoneidad` | PILOTO, ADMIN |

### 5.8 Misiones

Base: `/misiones`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/misiones` | Autenticado |
| GET | `/misiones?estado=PLANIFICADA` | Autenticado |
| GET | `/misiones/{id}` | Autenticado |
| POST | `/misiones` | Autenticado |
| PUT | `/misiones/{id}` | Autenticado |
| DELETE | `/misiones/{id}` | ADMIN |

**MisionRequest:**

| Campo | Tipo | Notas |
|-------|------|-------|
| nombre | string | Obligatorio |
| descripcion | string \| null | |
| fechaInicio | string (date) | ISO-8601 |
| fechaFin | string (date) \| null | |
| estado | EstadoMision | PLANIFICADA, EN_CURSO, EJECUTADA, CANCELADA |
| categoria | CategoriaMision | INSPECCION, VIGILANCIA, MAPEO, EMERGENCIA, OTRO |
| prioridad | PrioridadMision | BAJA, MEDIA, ALTA, CRITICA |
| dronId | number \| null | |
| pilotoId | number \| null | |
| siteId | number \| null | |

**MisionDTO** (respuesta): incluye `dronNombre`, `pilotoNombre`, `siteNombre` además de los campos del request.

### 5.9 Tareas

Base: `/tareas`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/tareas` | Autenticado |
| GET | `/tareas?estado=PENDIENTE` | Autenticado |
| GET | `/tareas/{id}` | Autenticado |
| POST | `/tareas` | Autenticado |
| PUT | `/tareas/{id}` | Autenticado |
| DELETE | `/tareas/{id}` | ADMIN |

**TareaRequest:**

| Campo | Tipo | Notas |
|-------|------|-------|
| titulo | string | Obligatorio |
| descripcion | string \| null | |
| estado | EstadoTarea | PENDIENTE, EN_PROGRESO, COMPLETADA, CANCELADA |
| prioridad | PrioridadTarea | BAJA, MEDIA, ALTA, CRITICA |
| fechaVencimiento | string (date) \| null | |
| asignadoAId | number \| null | ID de usuario |
| misionId | number \| null | |

### 5.10 Alertas

Base: `/alertas`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/alertas` | Autenticado |
| GET | `/alertas/activas` | Autenticado |
| GET | `/alertas/{id}` | Autenticado |
| PUT | `/alertas/{id}/resolver` | Autenticado |
| DELETE | `/alertas/{id}` | ADMIN |

Las alertas se generan automáticamente por el sistema (`AlertaScheduler`) al detectar licencias próximas a vencer, CMAs vencidas, etc. No se crean manualmente desde el frontend.

**Alerta (respuesta):**

| Campo | Tipo | Notas |
|-------|------|-------|
| id | number | |
| tipo | TipoAlerta | LICENCIA_VENCIDA, CMA_VENCIDA, MANTENIMIENTO_PENDIENTE, etc. |
| nivel | NivelAlerta | INFO, ADVERTENCIA, CRITICA |
| mensaje | string | |
| subtitulo | string \| null | |
| resuelta | boolean | |
| fechaCreacion | string (datetime) | ISO-8601 |

### 5.11 Logs (Libros de vuelo)

Base: `/logs`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/logs` | Autenticado |
| GET | `/logs?entidadTipo=DRON&entidadId=5` | Autenticado |
| GET | `/logs/{id}` | Autenticado |
| POST | `/logs` | Autenticado |
| DELETE | `/logs/{id}` | ADMIN |

**LogRequest:**

| Campo | Tipo | Notas |
|-------|------|-------|
| entidadTipo | string | DRON, DOCK, MISION, USUARIO, etc. |
| entidadId | number | ID de la entidad referenciada |
| tipo | string | VUELO, INCIDENTE, MANTENIMIENTO, OBSERVACION, etc. |
| detalle | string | Texto del log |
| operadorId | number \| null | ID del usuario operador |
| timestamp | string (datetime) \| null | Si null, backend usa now() |

**LogDTO** (respuesta): incluye `entidadTipo`, `entidadId`, `tipo`, `detalle`, `operadorNombre`, `timestamp`.

### 5.12 Mantenimientos

Base: `/mantenimientos`

#### Drones

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/mantenimientos/drones` | Autenticado |
| GET | `/mantenimientos/drones?dronId={id}` | Autenticado |
| GET | `/mantenimientos/drones/{id}` | Autenticado |
| POST | `/mantenimientos/drones` | Autenticado |
| PUT | `/mantenimientos/drones/{id}` | Autenticado |
| DELETE | `/mantenimientos/drones/{id}` | ADMIN |

**MantenimientoDronRequest:**

| Campo | Tipo | Notas |
|-------|------|-------|
| dronId | number | Obligatorio |
| usuarioId | number | Obligatorio |
| fechaMantenimiento | string (date) | Obligatorio |
| bateriaViejaId | number \| null | |
| bateriaNuevaId | number \| null | |
| helicesViejasIds | number[] \| null | |
| helicesNuevasIds | number[] \| null | |
| observaciones | string \| null | |
| fotos | string \| null | |

#### Docks

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/mantenimientos/docks` | Autenticado |
| GET | `/mantenimientos/docks?dockId={id}` | Autenticado |
| GET | `/mantenimientos/docks/{id}` | Autenticado |
| POST | `/mantenimientos/docks` | Autenticado |
| PUT | `/mantenimientos/docks/{id}` | Autenticado |
| DELETE | `/mantenimientos/docks/{id}` | ADMIN |

**MantenimientoDockRequest:**

| Campo | Tipo | Notas |
|-------|------|-------|
| dockId | number | Obligatorio |
| usuarioId | number | Obligatorio |
| fechaMantenimiento | string (date) | Obligatorio |
| observaciones | string \| null | |
| fotos | string \| null | |

### 5.13 Mapa de equipos

Base: `/mapa`

| Método | Ruta | Roles |
|--------|------|-------|
| GET | `/mapa/equipos` | Autenticado |

Devuelve lista de equipos con coordenadas (lat/lon) para mostrar en el mapa. Incluye drones y docks con `tipo`, `nombre`, `estado`, `lat`, `lon`.

### 5.14 Clima (OpenWeatherMap) — PÚBLICO

Base: `/clima`

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/clima` | No | Último registro de todos los sites |
| GET | `/clima/{codigo}` | No | Último registro de un site específico |
| GET | `/clima/{codigo}/historial?limit=24` | No | Historial de un site (default 24 registros) |

**ClimaDTO (respuesta):**

| Campo | Tipo | Notas |
|-------|------|-------|
| id | number | ID del registro |
| codigo | string | Código del site: EFO, CL, CAM |
| siteName | string | Nombre completo del site |
| cityName | string | Nombre de ciudad según OWM |
| tempCelsius | number | Temperatura en °C |
| windSpeedMs | number | Velocidad del viento en m/s |
| windGustMs | number | Ráfagas en m/s (0 si no hay dato) |
| visibilityMeters | number | Visibilidad en metros |
| conditionMain | string | Tipo OWM: Clouds, Rain, Clear, Thunderstorm, etc. |
| conditionDesc | string | Descripción en español (ej. "muy nuboso") |
| isFlyable | boolean | `true` si condiciones aptas para volar |
| recordedAt | string (datetime) | ISO-8601, timestamp del servidor |

**Lógica `isFlyable` (backend):**
- `false` si `windSpeedMs > 100`
- `false` si `conditionMain` ∈ `{Rain, Thunderstorm, Snow, Drizzle, Tornado, Squall}`
- `false` si `visibilityMeters < 500`
- `true` en cualquier otro caso

**Sites configurados:**

| Código | Nombre | Lat | Lon |
|--------|--------|-----|-----|
| EFO | Estación Fernandez Oro | -39.01 | -67.88 |
| CL | Cañadón León | -46.64617 | -67.71842 |
| CAM | Cañadón Amarillo | -37.4687 | -69.0275 |

El scheduler actualiza los datos cada **5 minutos** y los persiste en la tabla `clima_registros` con FK a `sites`.

---

## 6. Modelos de datos

### 6.1 Site (entidad actualizada)

| Campo | Tipo | Notas |
|-------|------|-------|
| id | number | Long |
| codigo | string \| null | Único. Valores: `EFO`, `CL`, `CAM` (más en el futuro) |
| nombre | string | |

> El campo `codigo` fue agregado en v3.0. Los sites existentes tienen: id=1 CAM, id=2 EFO, id=3 CL.

### 6.2 Usuario

| Campo | Tipo | Notas |
|-------|------|-------|
| id | number | |
| nombre | string | |
| apellido | string \| null | |
| email | string | Usado como login |
| roles | Role[] | |
| estado | string | PENDIENTE_APROBACION, ACTIVO, DESACTIVADO |
| dni | string \| null | |
| horasVuelo | number \| null | |
| cantidadVuelos | number \| null | |
| passwordMission | string \| null | Máx 30 chars |
| activo | boolean | |

### 6.3 Mision (DTO)

| Campo | Tipo |
|-------|------|
| id | number |
| nombre | string |
| descripcion | string \| null |
| fechaInicio | string (date) |
| fechaFin | string (date) \| null |
| estado | EstadoMision |
| categoria | CategoriaMision |
| prioridad | PrioridadMision |
| dronId / dronNombre | number / string |
| pilotoId / pilotoNombre | number / string |
| siteId / siteNombre | number / string |

### 6.4 Tarea (DTO)

| Campo | Tipo |
|-------|------|
| id | number |
| titulo | string |
| descripcion | string \| null |
| estado | EstadoTarea |
| prioridad | PrioridadTarea |
| fechaVencimiento | string (date) \| null |
| asignadoAId / asignadoANombre | number / string |
| misionId / misionNombre | number / string |

### 6.5 MantenimientoDronDTO

| Campo | Tipo |
|-------|------|
| id | number |
| dronId / dronNombre / dronModelo | |
| usuarioId / usuarioNombre | |
| fechaMantenimiento | string (date) |
| bateriaViejaId / bateriaNuevaId | number \| null |
| helicesViejasIds / helicesNuevasIds | number[] \| null |
| observaciones | string \| null |
| fotos | string \| null |

### 6.6 MantenimientoDockDTO

| Campo | Tipo |
|-------|------|
| id | number |
| dockId / dockNombre / dockModelo | |
| usuarioId / usuarioNombre | |
| fechaMantenimiento | string (date) |
| observaciones | string \| null |
| fotos | string \| null |

### 6.7 Compra, Proveedor, Licencia, Seguro, Accesorios

Ver secciones 6.12–6.27 de la versión anterior de este informe (sin cambios desde v2.0).

---

## 7. Enums

### EstadoMision
`PLANIFICADA` | `EN_CURSO` | `EJECUTADA` | `CANCELADA`

### CategoriaMision
`INSPECCION` | `VIGILANCIA` | `MAPEO` | `EMERGENCIA` | `OTRO`

### PrioridadMision / PrioridadTarea
`BAJA` | `MEDIA` | `ALTA` | `CRITICA`

### EstadoTarea
`PENDIENTE` | `EN_PROGRESO` | `COMPLETADA` | `CANCELADA`

### NivelAlerta
`INFO` | `ADVERTENCIA` | `CRITICA`

### TipoAlerta
`LICENCIA_VENCIDA` | `CMA_VENCIDA` | `MANTENIMIENTO_PENDIENTE` | `SEGURO_VENCIDO` | `BATERIA_BAJA` | `OTRO`

### Estado (equipos)
`STOCK_ACTUAL` | `EN_PROCESO` | `STOCK_ACTIVO` | `EN_DESUSO`

### EstadoUsuario
`PENDIENTE_APROBACION` | `ACTIVO` | `DESACTIVADO`

### TipoCompra
`LICENCIA_SW` | `REPUESTO` | `COMBUSTIBLE` | `VIATICO` | `SEGURO` | `EQUIPO` | `OTRO`

### MetodoPago
`EFECTIVO` | `TRANSFERENCIA` | `TARJETA` | `OTRO`

### TipoEquipo
`DRON` | `DOCK` | `BATERIA` | `HELICE` | `ANTENA_RTK` | `ANTENA_STARLINK` | `OTRO`

### conditionMain (OWM — clima)
`Thunderstorm` | `Drizzle` | `Rain` | `Snow` | `Atmosphere` | `Clear` | `Clouds` | `Tornado` | `Squall`

---

## 8. Códigos HTTP

| Código | Uso |
|--------|-----|
| 200 | OK |
| 201 | Created |
| 204 | No Content (DELETE, cambio de password) |
| 400 | Bad Request (validación, negocio) |
| 401 | Unauthorized (sin token o inválido) |
| 403 | Forbidden (sin rol) o cuenta inactiva en login |
| 404 | Not Found |
| 409 | Conflict (duplicado) |
| 500 | Error interno |

> **Importante:** Si `/docks` devuelve 403 con token válido, verificar que el token no haya expirado. El backend reiniciado invalida los tokens anteriores.

---

## 9. Roles y permisos

- **ROLE_ADMIN:** acceso completo.
- **ROLE_PILOTO:** mi-perfil completo (datos, CMA, licencias ANAC, password_mission).
- **ROLE_USER:** compras, licencias, seguros (CRUD salvo delete); mi-perfil básico.

---

## 10. Variables de entorno del backend

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Puerto (usar 8081 en dev local) |
| `DB_HOST` | postgres_qnt | Host PostgreSQL (usar `localhost` en dev local) |
| `DB_PORT` | 5432 | |
| `DB_NAME` | postgres_qnt | |
| `DB_USER` | qntgestion | |
| `DB_PASSWORD` | java12345 | |
| `JWT_SECRET` | MyVerySecretKey... | Cambiar en producción |
| `JWT_EXPIRATION_MS` | 3600000 | 1 hora |
| `OWM_API_KEY` | af7b3efbe262dec84eeb881a6cb69d10 | OpenWeatherMap API Key |
| `OWM_EFO_LAT/LON` | -39.01 / -67.88 | Coords EFO |
| `OWM_CL_LAT/LON` | -46.64617 / -67.71842 | Coords Cañadón León |
| `OWM_CAM_LAT/LON` | -37.4687 / -69.0275 | Coords Cañadón Amarillo |

**Comando de inicio en desarrollo:**
```bash
SERVER_PORT=8081 DB_HOST=localhost mvn spring-boot:run
```

---

## 11. Recomendaciones para el frontend

1. **Base URL:** Variable de entorno por ambiente apuntando a `/api/qnt/v1`.
2. **Interceptor HTTP:** Header `Authorization: Bearer <token>` en todas las peticiones protegidas.
3. **401:** Redirigir a login y limpiar token.
4. **403:** Mostrar "sin permisos". Si ocurre en `/docks` tras reinicio del backend, el token expiró — re-login.
5. **Clima:** Endpoint público, llamar sin token desde la landing. Refrescar cada 5 min en el frontend (el backend ya hace el fetch a OWM cada 5 min). Usar `isFlyable` para mostrar badge APTO/NO APTO.
6. **Fechas:** ISO-8601 (`yyyy-MM-dd` para fechas, `yyyy-MM-ddTHH:mm:ssZ` para timestamps).
7. **Listados:** No paginados (array completo).
8. **`conditionMain` → icono OWM:** Mapear a código de icono según tabla: `Thunderstorm→11d`, `Drizzle→09d`, `Rain→10d`, `Snow→13d`, `Atmosphere→50d`, `Clear→01d`, `Clouds→03d`.

---

## 12. Tabla de nuevos endpoints — resumen v3.0

| Endpoint | Auth | Novedades |
|----------|------|-----------|
| `GET /misiones` | Sí | CRUD completo + filtro por estado |
| `GET /tareas` | Sí | CRUD + filtro por estado; Kanban en frontend |
| `GET /alertas/activas` | Sí | Alertas automáticas del sistema |
| `GET /logs` | Sí | Libros de vuelo con filtro entidadTipo/entidadId |
| `GET /mantenimientos/drones` | Sí | Registro mantenimiento con cambio batería/hélices |
| `GET /mantenimientos/docks` | Sí | Registro mantenimiento dock |
| `GET /mapa/equipos` | Sí | Equipos geolocalizados para Leaflet |
| `GET /clima` | **No** | Último dato OWM por site, persistido en BD |
| `GET /clima/{codigo}/historial` | **No** | Historial clima por site |

---

*Documento actualizado — v3.0. Cubre backend completo incluyendo Misiones, Tareas, Alertas, Logs, Mantenimiento, Mapa y sistema de Clima con OpenWeatherMap.*
