# Blueprint — QNT Gestion

> Documento de arquitectura técnica del sistema de gestión operativa de drones (oil & gas). Describe el stack, la estructura del backend, las integraciones y las decisiones de diseño.

---

## Visión técnica

QNT Gestion es el sistema de trazabilidad operativa para flotas de drones DJI (Matrice 4TD, Dock 3) desplegadas en yacimientos petroleros. No reemplaza a FlightHub ni FlytBase: las misiones se crean allí y se **registran o vinculan** en este sistema para asociar piloto, dock, dron, pozo y parámetros. El backend es la **fuente de verdad** de stock, mantenimientos, pilotos, sites y misiones; N8N y futuros flujos (Telegram, actualización de datos DJI) consumen la **misma API y la misma base de datos**.

La arquitectura prioriza un **monolito Spring Boot** organizado por subsistemas (stock, mantenimiento, pilotos, sites, misiones, compras), con API REST versionada y documentación OpenAPI. Escalabilidad pensada para crecer en cantidad de docks y sites sin fragmentar el sistema en microservicios por ahora.

---

## Stack tecnológico

| Capa | Tecnología | Razón |
|------|------------|--------|
| Backend | Java 21 + Spring Boot 4.x | Ya adoptado; ecosistema maduro, JPA, Security, validación. |
| API | Spring Web MVC + SpringDoc OpenAPI | REST estándar; documentación interactiva (Swagger UI). |
| Seguridad | Spring Security | Roles (admin, piloto, mantenimiento), JWT o sesión según necesidad. |
| Persistencia | Spring Data JPA + PostgreSQL | Modelo relacional (empresa → site → dock → dron, activos, mantenimientos). |
| Validación | Bean Validation | Reglas en entidades y DTOs. |
| Build | Maven (wrapper en `gestion/`) | Compilación, tests, empaquetado WAR. |
| Base de datos | PostgreSQL | Producción y desarrollo. ACID, integridad referencial. |
| Tests | JUnit 5 + Spring Boot Test + H2 (test) | Tests de contexto e integración; H2 solo en scope test. |
| Frontend | Por definir (Vue 3 / React) | SPA único que consumirá esta API. |
| Integraciones | N8N (flujos), misma BD | N8N llama a esta API y lee/escribe en la misma BD (usuarios, misiones, etc.). DJI vía N8N en fase posterior. |
| Despliegue | Docker (recomendado) | Contenedor para app + PostgreSQL; después evaluar K8s o PaaS. |
| CI/CD | Por definir | Sin pipeline en repo aún. |

---

## Estructura del proyecto

```
QNT-JavaSpring/
│
├── gestion/                              # Backend Spring Boot
│   ├── src/main/java/com/gestion/qnt/
│   │   ├── GestionApplication.java       # Arranque
│   │   ├── config/                       # Security, OpenAPI, etc.
│   │   ├── domain/                       # Entidades JPA (o model/entity)
│   │   │   ├── Estado.java               # Enum
│   │   │   ├── Empresa.java, Site.java
│   │   │   ├── Dock.java, Dron.java
│   │   │   ├── Bateria.java, Helice.java
│   │   │   ├── Licencia.java, AntenaRtk.java, AntenaStarlink.java
│   │   │   ├── MantenimientoDock.java, MantenimientoDron.java
│   │   │   ├── Instalacion.java
│   │   │   ├── Usuario.java
│   │   │   ├── Pozo.java, Mision.java
│   │   │   └── Log.java
│   │   ├── repository/                   # Spring Data JPA
│   │   ├── service/                      # Lógica de negocio por subsistema
│   │   │   ├── stock/
│   │   │   ├── mantenimiento/
│   │   │   ├── pilotos/
│   │   │   ├── sites/
│   │   │   ├── misiones/
│   │   │   └── compras/                  # Fase posterior
│   │   ├── controller/                  # REST por recurso
│   │   │   └── api/v1/
│   │   └── dto/                         # Request/Response
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── application-prod.properties
│   └── pom.xml
│
├── docs/
│   ├── DOMINIO.md                       # Reglas de negocio
│   └── ENTIDADES.md                     # Modelo de datos (atributos)
│
├── Importante-main/                     # Kit documentación y agentes
├── README.md
└── BLUEPRINT.md                        # Este archivo
```

La lista completa de entidades y atributos está en [docs/ENTIDADES.md](docs/ENTIDADES.md).

---

## Flujo de datos principal

### Registro de misión (después de crearla en FlightHub/FlytBase)

```
Usuario (front) → POST /api/v1/misiones → Controller → Service → Repository → PostgreSQL
                                                                        ↑
                                                    N8N / otros clientes pueden llamar la misma API
```

1. El usuario crea la misión en FlightHub o FlytBase.
2. En QNT Gestion registra o vincula la misión: piloto, dock, dron, pozo, link RTSP, descripción.
3. La API valida (piloto existe, dock/dron asociados, etc.) y persiste.
4. N8N puede consumir la misma API para flujos (ej. Telegram) usando usuarios y contraseñas de esta BD.

### Cambio de estado de un activo (ej. batería)

```
Usuario → PATCH /api/v1/baterias/:id → Estado actualizado (Stock actual → En proceso → Stock activo)
                                    → Se persiste Instalación / log para trazabilidad
```

### Datos DJI (fase posterior)

```
N8N (periódico) → Consulta API DJI (ciclos batería, %, etc.) → Escribe en esta BD (misma PostgreSQL)
```

El backend **no** llama a la API de DJI; N8N orquesta y actualiza la BD.

---

## Módulos principales (subsistemas)

### Stock y activos

**Responsabilidad:** CRUD y estados de Dock, Dron, Batería, Hélice, Licencia, Antena RTK, Antena Starlink. Transiciones de estado (Stock actual ↔ En proceso ↔ Stock activo ↔ En desuso) con trazabilidad (Instalación, logs).  
**API:** REST sobre `/api/v1/docks`, `/api/v1/drones`, `/api/v1/baterias`, `/api/v1/helices`, `/api/v1/licencias`, etc.  
**Dependencias:** Dominio (entidades), repositorios, reglas en [DOMINIO.md](docs/DOMINIO.md).

### Mantenimiento

**Responsabilidad:** Registro de MantenimientoDock y MantenimientoDron (usuario, fecha, observaciones, fotos). Checklists como guía en front; no obligatoriamente como estructura de datos en backend.  
**API:** `/api/v1/mantenimientos/dock`, `/api/v1/mantenimientos/dron` (o anidado en dock/dron).  
**Dependencias:** Stock (dock, dron), Usuario.

### Pilotos y usuarios

**Responsabilidad:** Usuarios, roles, pilotos con CMA (vencimiento 3 años, imágenes), horas de vuelo. Contraseña usada por N8N/Telegram.  
**API:** `/api/v1/usuarios`, `/api/v1/auth` (login, etc.).  
**Dependencias:** Dominio Usuario; Spring Security.

### Sites

**Responsabilidad:** Empresa (Quintana), Sites (EFO, Cañadón Amarillo, etc.), responsables.  
**API:** `/api/v1/empresas`, `/api/v1/sites`.  
**Dependencias:** Dominio Empresa, Site.

### Misiones

**Responsabilidad:** Registro y consulta de misiones (nombre, piloto, dock/dron, pozo, link RTSP, descripción, estado). Origen de la misión en FlightHub/FlytBase; este sistema solo vincula y traza.  
**API:** `/api/v1/misiones`, `/api/v1/pozos`.  
**Dependencias:** Usuario, Dock/Dron, Pozo.

### Compras / facturación (fase posterior)

**Responsabilidad:** Registro de gastos, facturas, combustible, viáticos, seguros (quién, cuánto, a quién). Sin generación de facturas ni reportes IVA en la primera fase.  
**API:** A definir.  
**Dependencias:** Entidades de compras (ver [ENTIDADES.md](docs/ENTIDADES.md)).

---

## Decisiones de arquitectura (ADRs)

### ADR-001: Monolito Spring Boot por subsistemas

**Estado:** Aceptado  

**Contexto:** Múltiples subsistemas (stock, mantenimiento, pilotos, sites, misiones, compras) con datos fuertemente relacionados (dock → dron → baterías, mantenimientos, misiones). Equipo interno, escala acotada (decenas de docks/sites).

**Decisión:** Un solo artefacto Spring Boot, código organizado por paquetes/dominios (service, controller por subsistema). No microservicios.

**Consecuencias:** Un despliegue, una BD, menos operación distribuida. Cambios que cruzan subsistemas en un solo repo. Si en el futuro la escala o equipos lo exigen, se puede extraer un subsistema a otro servicio.

---

### ADR-002: PostgreSQL como única BD; N8N usa la misma BD

**Estado:** Aceptado  

**Contexto:** Trazabilidad, integridad referencial (dock–dron–site–empresa, mantenimientos, misiones). N8N necesita usuarios y contraseñas para flujos (ej. Telegram); el equipo definió que N8N use la misma base que este sistema.

**Decisión:** PostgreSQL para QNT Gestion. N8N se conecta a la misma BD para leer usuarios/datos y puede llamar a la API REST de este backend.

**Consecuencias:** Una sola fuente de verdad. Cuidado con esquemas/tablas que N8N modifique directamente; preferible que N8N use la API para escrituras y la BD solo para lecturas o datos que comparten.

---

### ADR-003: No integrar DJI API en el backend (por ahora)

**Estado:** Aceptado  

**Contexto:** La API de DJI aporta datos (ciclos de batería, %, etc.). Aún no está claro el detalle de uso; el equipo prefiere no acoplar el backend a DJI directamente.

**Decisión:** El backend no llama a la API de DJI. En una fase posterior, un flujo de N8N consultará DJI y actualizará esta BD de forma periódica.

**Consecuencias:** Menor acoplamiento y menos lógica de integración en el backend. La actualización de datos DJI depende de N8N y de la definición del flujo.

---

### ADR-004: API REST versionada y OpenAPI

**Estado:** Aceptado  

**Contexto:** Frontend y N8N consumen la API. Se esperan cambios en contratos con el tiempo.

**Decisión:** Rutas bajo `/api/v1/`. SpringDoc OpenAPI para documentación y Swagger UI.

**Consecuencias:** Posibilidad de introducir v2 sin romper clientes. Documentación siempre al día con el código.

---

## Principios de diseño

1. **Trazabilidad primero:** Cambios de estado en activos, mantenimientos e instalaciones deben quedar registrados (usuario, fecha, observaciones; tabla de auditoría o logs cuando corresponda).
2. **Este sistema es la fuente de verdad operativa:** Misiones se crean en FH/FlytBase pero se registran aquí; usuarios y contraseñas viven aquí; N8N depende de esta BD/API.
3. **No inventar datos:** Lo que no provee el usuario o una integración definida no se asume; campos opcionales o `[PENDIENTE]` en documentación.
4. **Estados explícitos:** El enum de estado (Stock actual, En proceso, Stock activo, En desuso) se reutiliza en todos los activos para reportes y filtros consistentes.
5. **Un documento a la vez:** Documentación (README, DOMINIO, ENTIDADES, BLUEPRINT) se mantiene al día de forma incremental.

---

## Limitaciones conocidas

- **Sin integración directa con DJI:** Los datos de batería/dron desde DJI llegarán vía N8N actualizando la BD; no hay job ni servicio en el backend que llame a DJI.
- **Compras e IVA en fase posterior:** Solo registro de gastos/facturas; sin generación de facturas ni reportes IVA por ahora.
- **CI/CD no definido:** No hay pipeline en el repo; agregar cuando se defina el flujo de despliegue.
- **Frontend eliminado (droneops-front):** El front se reconstruirá; tecnología (Vue/React) por definir.
- **Regla “90 días sin vuelo”:** No definida aún (ANAC vs política interna); dejar abierto en dominio y en implementación.

---

## Referencias

- [README.md](README.md) — Visión general, instalación, uso.
- [docs/DOMINIO.md](docs/DOMINIO.md) — Reglas de negocio y decisiones de producto.
- [docs/ENTIDADES.md](docs/ENTIDADES.md) — Entidades y atributos del modelo de datos.
- Spring Boot 4.x: https://docs.spring.io/spring-boot/
- SpringDoc OpenAPI: https://springdoc.org/
