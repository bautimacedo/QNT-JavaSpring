# Blueprint — StoreFront

> ⚠️ Este es un archivo de EJEMPLO. Reemplazarlo con el blueprint real del proyecto.

---

## 🎯 Visión técnica

StoreFront es una plataforma de e-commerce headless diseñada para pequeños y medianos comercios. El sistema separa completamente la capa de presentación del backend, permitiendo que distintos frontends (web, mobile, PWA) consuman la misma API.

La arquitectura prioriza la simplicidad operacional sobre la escala masiva: está diseñada para ser desplegada y mantenida por equipos pequeños sin infraestructura compleja.

---

## 🏗️ Stack tecnológico

| Capa | Tecnología | Razón de la elección |
|------|-----------|---------------------|
| Frontend | React 18 + TypeScript | Ecosistema maduro, tipado estático |
| Estilos | Tailwind CSS | Velocidad de desarrollo, consistencia |
| Estado global | Zustand | Más simple que Redux para este scope |
| Backend API | Node.js + Fastify | Performance, schema validation nativa |
| ORM | Prisma | Migraciones typesafe, DX superior |
| Base de datos | PostgreSQL | ACID, soporte JSON, maduro |
| Storage | AWS S3 | Estándar de industria para assets |
| Pagos | Stripe | API excelente, webhook reliability |
| Emails | Resend | Simple, reliable, buen DX |
| Testing | Vitest + Playwright | Unificado con Vite, E2E robusto |
| CI/CD | GitHub Actions | Integrado con el repo |
| Deploy | Railway | Simplicidad sobre flexibilidad |

---

## 📁 Estructura del proyecto

```
storefront/
│
├── apps/
│   ├── web/                    # Frontend React (cliente)
│   └── admin/                  # Dashboard administrativo
│
├── packages/
│   ├── api/                    # Fastify API server
│   │   ├── src/
│   │   │   ├── routes/         # Endpoints por dominio
│   │   │   ├── services/       # Lógica de negocio
│   │   │   ├── plugins/        # Plugins de Fastify
│   │   │   └── db/             # Prisma client y seeds
│   │   └── prisma/
│   │       └── schema.prisma
│   │
│   └── shared/                 # Tipos y utils compartidos
│
├── docker-compose.yml          # Dev environment
└── turbo.json                  # Monorepo config (Turborepo)
```

---

## 🔄 Flujo de datos principal (checkout)

```
Cliente Web → API REST → Service Layer → Prisma → PostgreSQL
                  ↓
             Stripe API (pago)
                  ↓
             Resend (email confirmación)
```

1. **Request:** El cliente envía el carrito y datos de pago a `POST /api/orders`
2. **Validación:** El schema de Fastify valida los datos de entrada
3. **Pago:** El service crea un PaymentIntent en Stripe
4. **Persistencia:** Si el pago es exitoso, se guarda la orden en PostgreSQL
5. **Notificación:** Se envía email de confirmación vía Resend
6. **Response:** La API devuelve el ID de orden y estado

---

## 🧩 Módulos principales

### Auth Module

**Responsabilidad:** Autenticación y autorización de usuarios y admins  
**Interfaz pública:** `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh`  
**Dependencias:** JWT (jose), bcrypt, PostgreSQL

### Catalog Module

**Responsabilidad:** Gestión de productos, categorías, inventario  
**Interfaz pública:** REST CRUD en `/api/products`, `/api/categories`  
**Dependencias:** PostgreSQL, S3 (para imágenes)

### Cart Module

**Responsabilidad:** Carrito de compras persistente por usuario  
**Interfaz pública:** `GET/POST/DELETE /api/cart`  
**Dependencias:** PostgreSQL, Catalog Module

### Orders Module

**Responsabilidad:** Proceso de checkout y gestión de órdenes  
**Interfaz pública:** `POST /api/orders`, `GET /api/orders/:id`  
**Dependencias:** Stripe, Resend, Cart Module, Catalog Module

---

## 🔑 Decisiones de arquitectura

### ADR-001: Monorepo con Turborepo

**Fecha:** Diciembre 2024  
**Estado:** Aceptado

**Contexto:** El proyecto tiene frontend, admin y API que comparten tipos y lógica. Mantenerlos como repos separados generaría overhead de sincronización.

**Decisión:** Usar Turborepo para gestionar el monorepo con caching de builds inteligente.

**Consecuencias:** 
- (+) Un solo repo, un solo PR para cambios que afectan múltiples apps
- (+) Tipos compartidos sin publicar paquetes npm
- (-) Setup inicial más complejo para nuevos contribuidores

### ADR-002: PostgreSQL sobre MongoDB

**Fecha:** Diciembre 2024  
**Estado:** Aceptado

**Contexto:** Los datos del e-commerce son inherentemente relacionales (órdenes → items → productos → categorías).

**Decisión:** PostgreSQL con Prisma. Aprovechar JSONB para configuraciones flexibles de productos.

**Consecuencias:**
- (+) Integridad referencial garantizada
- (+) Queries complejas con JOINs eficientes
- (-) Menos flexibilidad para catálogos con atributos muy variables

---

## ⚡ Principios de diseño

1. **Simple primero:** No over-engineer. Solución más simple que funcione.
2. **Typesafe end-to-end:** TypeScript en cliente y servidor, tipos compartidos.
3. **Errores explícitos:** Los errores de negocio son tipos, no excepciones.
4. **Test lo que importa:** Tests de integración > tests unitarios aislados.

---

## 🚫 Limitaciones conocidas

- **Sin WebSockets:** Las actualizaciones de stock son eventual-consistent (polling cada 30s). Suficiente para el MVP.
- **Single region:** El deploy en Railway es single-region. Para latencia global, se necesitaría un CDN más agresivo o multi-region.
- **Search básico:** La búsqueda full-text de PostgreSQL es suficiente hasta ~100k productos. Después, migrar a Meilisearch (en roadmap v0.4.0).
