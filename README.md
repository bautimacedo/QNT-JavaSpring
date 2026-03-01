# QNT Gestión — Sistema de gestión operativa de drones (Oil & Gas)

> Sistema para mantener la trazabilidad de stock, pilotos, compras, mantenimientos y misiones de flotas de drones DJI (Matrice 4TD, Dock 3) desplegadas en yacimientos petroleros. Integra FlightHub2 / FlytBase para misiones y da soporte a escala (múltiples docks, sites y empresas).

[![Estado](https://img.shields.io/badge/estado-en_desarrollo-yellow)]()
[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?logo=spring)]()

---

## Contexto

El sistema es utilizado por una empresa que presta servicios a operadoras del sector oil & gas. Se gestionan drones y docks en sitios como **Cañadón Amarillo** (Mendoza), **Cañadón Seco** (Santa Cruz) y **Estación Fernández Oro** (Cipoletti). Incluye inspección de pozos, seguridad (detección de personas/vehículos) y, en este repositorio, la **gestión operativa**: inventario, mantenimientos, pilotos, compras, misiones y logs.

### Módulos / subsistemas

| Módulo | Descripción |
|--------|-------------|
| **Stock y activos** | Dock, dron, baterías, hélices, antenas RTK/Starlink, licencias (FlightHub/FlytBase), seguros. Estados: Stock actual, Stock activo, En mantenimiento, En desuso. |
| **Mantenimiento** | Mantenimiento preventivo de dock y dron (calibración GPS/IMU, cambio de hélices/baterías), logs y trazabilidad. |
| **Pilotos** | Usuarios con rol piloto, CMA, horas de vuelo, vencimientos (ej. 90 días sin vuelo). |
| **Misiones** | Misiones asociadas a dock/dron y piloto; parámetros y enlaces (RTSP, etc.) desde FlightHub/FlytBase. |
| **Sites** | Empresas → localizaciones (sites) → docks → drones. |
| **Compras / facturación** | Facturas, combustible, viáticos, seguros, IVA (en desarrollo). |
| **DJI API y logs** | Integración con API DJI para logs y datos de vuelo. |
| **N8N** | Automatizaciones que consumen esta API. |

---

## Estructura del repositorio

```
QNT-JavaSpring/
├── gestion/                 # Backend Spring Boot (API REST, JPA, Security)
│   ├── src/main/java/       # Código fuente
│   ├── src/main/resources/  # application.properties, perfiles (dev/prod)
│   └── pom.xml              # Java 21, Spring Boot 4.x, PostgreSQL, SpringDoc OpenAPI
└── Importante-main/         # Kit de documentación y agentes (CEO, PM, Programador)
    ├── files/               # Plantillas README, ROADMAP, BLUEPRINT, CHANGELOG, AGENT_PROMPT
    └── Agente* /             # Prompts y flujos por tipo de agente
```

La documentación de arquitectura detallada estará en [BLUEPRINT.md](./BLUEPRINT.md) (por crear).

---

## Requisitos previos

- **Java 21**
- **Maven 3.x** (o usar el wrapper incluido en `gestion/`)
- **PostgreSQL** (para ejecución local o con perfil `dev`/`prod`)

---

## Instalación y ejecución

```bash
git clone https://github.com/bautimacedo/QNT-JavaSpring.git
cd QNT-JavaSpring/gestion
```

Configurá la base de datos (ver más abajo) y luego:

```bash
./mvnw spring-boot:run
```

Documentación OpenAPI (Swagger): **http://localhost:8080/swagger-ui.html** (puerto por defecto 8080).

### Configuración

En `src/main/resources/application.properties` o `application-dev.properties` definí la conexión a PostgreSQL. Ejemplo (no subir credenciales al repo):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/qnt_gestion
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
```

Para pruebas automáticas se usa una base en memoria (H2) solo dentro de los tests; no hace falta configurarla.

---

## Tests

```bash
cd gestion
./mvnw test
```

### Agente Tester y pipeline

- **Desde Cursor:** para que un agente ejecute la batería de tests y reporte, invocá:  
  **"Lee Importante-main/AgenteQA/AGENTE_TESTER.md y ejecutá la verificación"**.  
  (También vale: **"Lee agent-bootstrap/AGENTE_TESTER.md y ejecutá la verificación"** — redirige al mismo agente.)  
  Eso corre `mvn test` y, si la app está levantada, el smoke test de la API (`./scripts/smoke-test-api.sh`).

- **Pipeline secuencial (PM → Programador → Tester):** solo le hablás al PM; el PM invoca al Programador y el Programador al Tester. Ver [Importante-main/PIPELINE_SECUENCIAL.md](./Importante-main/PIPELINE_SECUENCIAL.md).

- **En el pipeline (GitHub Actions):** en cada push y en cada PR a `main`/`master` se ejecuta el mismo flujo de tests (build + JUnit). Ver [.github/workflows/test.yml](./.github/workflows/test.yml).

---

## Roadmap y documentación

- **Dominio y reglas de negocio:** [docs/DOMINIO.md](./docs/DOMINIO.md)
- **Entidades y atributos (modelo de datos):** [docs/ENTIDADES.md](./docs/ENTIDADES.md)
- **Roadmap:** [ROADMAP.md](./ROADMAP.md) (por crear)
- **Arquitectura:** [BLUEPRINT.md](./BLUEPRINT.md)
- **Changelog:** [CHANGELOG.md](./CHANGELOG.md) (por crear)

---

## Contribuir

Flujo para el equipo interno:

1. Crear rama: `git checkout -b feature/nombre-feature`
2. Commit: `git commit -m 'feat: descripción'`
3. Push: `git push origin feature/nombre-feature`
4. Abrir Pull Request (o merge a `master` según el flujo del equipo)

---

## Licencia

Uso interno — **QNT Gestion**.
