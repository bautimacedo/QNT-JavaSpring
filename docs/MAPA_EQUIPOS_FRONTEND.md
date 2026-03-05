# v0.17.0 — Mapa de equipos: descripción e integración frontend

## Qué se implementó

Se añadió soporte para **ubicar equipos en un mapa** y exponer esos datos vía API.

### En el backend

1. **Coordenadas en equipos**  
   En las entidades **Dock, Dron, Bateria, Helice, AntenaRtk y AntenaStarlink** se agregaron tres campos opcionales (nullable):
   - **latitud** (BigDecimal, ej. -34.6037)
   - **longitud** (BigDecimal, ej. -58.3816)
   - **altitud** (BigDecimal, metros sobre referencia)

   Las columnas se crean/actualizan con `ddl-auto=update`. Los CRUD existentes (POST/PUT/GET) ya aceptan y devuelven estos campos.

2. **Validación de coordenadas**  
   Al crear o editar cualquier equipo, si se envían coordenadas se valida:
   - Latitud entre **-90 y 90**
   - Longitud entre **-180 y 180**  
   Si no se cumple, el backend responde **400** con un mensaje claro (ej. "La latitud debe estar entre -90 y 90").

3. **Endpoint para el mapa**  
   - **GET** `/api/qnt/v1/mapa/equipos`  
   - Requiere autenticación (header `Authorization: Bearer <token>`).
   - Devuelve un **array de marcadores**: solo equipos que tengan **latitud y longitud no nulas** (altitud puede ser null).
   - Cada elemento del array tiene:
     - `tipoEquipo`: `"DOCK"` | `"DRON"` | `"BATERIA"` | `"HELICE"` | `"ANTENA_RTK"` | `"ANTENA_STARLINK"`
     - `id`, `nombre`, `latitud`, `longitud`, `altitud` (puede ser null)
     - `estado`: estado del equipo (ej. STOCK_ACTIVO, EN_MANTENIMIENTO)
     - `ultimoMantenimiento`: fecha (LocalDate) o null; solo Dock y Dron lo tienen informado
     - `siteNombre`: nombre del site (solo Dock); null en el resto
     - `numeroSerie`: si existe

   Equipos **sin** lat/lng no aparecen en este endpoint.

---

## Por qué se implementó

El objetivo es poder **mostrar en un mapa interactivo** todos los equipos que están “en el campo” (con ubicación conocida), con su estado y datos útiles para el popup (nombre, último mantenimiento, site, etc.), **sin hacer una llamada por cada equipo**.  
Así el frontend puede:

- Pintar todos los marcadores de una vez.
- Diferenciar por tipo (icono/color según `tipoEquipo`).
- Mostrar en el popup nombre, estado, último mantenimiento y site (Dock) sin llamadas extra.

Las coordenadas son **opcionales**: equipos sin ubicación siguen funcionando igual en el resto de la app y simplemente no aparecen en el mapa.

---

## Cómo debe ser el frontend

### 1. Pantalla / vista “Mapa de equipos”

- **Ruta sugerida:** por ejemplo `/mapa` o `/equipos/mapa`.
- **Acceso:** usuario autenticado (el endpoint exige token).
- **Contenido principal:** un mapa (Leaflet, Mapbox, Google Maps, etc.) que:
  - Al cargar la vista llama a **GET** `/api/qnt/v1/mapa/equipos` con el token.
  - Por cada elemento del array coloca un **marcador** en `(latitud, longitud)`.
  - Usa **`tipoEquipo`** para elegir icono o color (ej. un icono distinto para DOCK, DRON, BATERIA, etc.).
  - Al hacer **click** en un marcador muestra un **popup** (o tooltip) con:
    - Nombre del equipo
    - Estado
    - Último mantenimiento (si existe)
    - Site (si es Dock)
    - Número de serie (si existe)
    - Opcional: enlace a la ficha del equipo (ej. `/docks/{id}`, `/drones/{id}` según `tipoEquipo`).

No hace falta ninguna otra llamada para pintar el mapa: un solo GET trae todos los marcadores con la información necesaria para el popup.

### 2. Dónde se cargan/editan las coordenadas

Las coordenadas se gestionan en los **formularios de alta y edición** de cada equipo (Dock, Dron, Bateria, Helice, AntenaRtk, AntenaStarlink):

- **Crear equipo:** el formulario puede tener tres campos opcionales: Latitud, Longitud, Altitud (numéricos). Si el usuario los completa, se envían en el body del POST al endpoint correspondiente (ej. POST `/api/qnt/v1/docks`).
- **Editar equipo:** en la pantalla de detalle/edición del equipo, los mismos campos (latitud, longitud, altitud) deben mostrarse y poder editarse; al guardar se envían en el PUT (ej. PUT `/api/qnt/v1/docks` con el body que incluya `latitud`, `longitud`, `altitud`).

Si no se envían, quedan null y el equipo no aparecerá en el mapa. Si se envían valores inválidos (ej. latitud 95), el backend responde **400** con el mensaje de validación; el frontend puede mostrar ese mensaje junto al formulario.

### 3. Flujo de uso recomendado

1. **Dar de alta o editar un equipo** (Dock, Dron, etc.) y, si se conoce la ubicación, completar latitud, longitud y opcionalmente altitud.
2. **Abrir la vista “Mapa de equipos”**: el mapa se pinta con todos los equipos que tengan lat/lng; el usuario ve solo los que tienen coordenadas.
3. **Click en un marcador**: se abre el popup con nombre, estado, último mantenimiento, site (Dock), etc.; opcionalmente un enlace a la ficha del equipo para ver/editar más datos (incluidas las coordenadas).

### 4. Detalles técnicos para el frontend

- **URL del endpoint:** `GET /api/qnt/v1/mapa/equipos` (misma base que el resto de la API).
- **Headers:** `Authorization: Bearer <token>` (el mismo que se usa para el resto de endpoints autenticados).
- **Respuesta:** JSON array de objetos con la estructura descrita arriba (`tipoEquipo`, `id`, `nombre`, `latitud`, `longitud`, `altitud`, `estado`, `ultimoMantenimiento`, `siteNombre`, `numeroSerie`).
- **Coordenadas en CRUD:** en POST/PUT de docks, drones, baterias, helices, antenas-rtk, antenas-starlink el body puede incluir `latitud`, `longitud`, `altitud` (números); son opcionales. Las respuestas de GET (listado y detalle) ya incluyen estos campos.

---

## Resumen

| Qué | Dónde / cómo |
|-----|------------------|
| Ver equipos en el mapa | Vista “Mapa”, GET `/api/qnt/v1/mapa/equipos`, pintar un marcador por elemento usando `tipoEquipo` para icono/color. |
| Popup del marcador | Nombre, estado, último mantenimiento, site (Dock), numeroSerie; opcional enlace a ficha del equipo. |
| Cargar/editar coordenadas | En formularios de alta y edición de Dock, Dron, Bateria, Helice, AntenaRtk, AntenaStarlink; enviar `latitud`, `longitud`, `altitud` en el body (opcionales). |
| Validación | Si el backend devuelve 400 por coordenadas inválidas, mostrar el mensaje al usuario. |

Con esto el frontend puede ofrecer un mapa de equipos con una sola llamada y mantener las coordenadas desde los mismos flujos de alta/edición de cada tipo de equipo.
