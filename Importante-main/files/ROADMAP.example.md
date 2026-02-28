# Roadmap — StoreFront

> ⚠️ Este es un archivo de EJEMPLO. Reemplazarlo con el roadmap real del proyecto.

> Estado actual: **v0.3.1** — Última actualización: 2025-01

---

## ✅ Completado

### v0.1.0 — Foundation
> Diciembre 2024

- [x] Setup inicial del proyecto (Vite + React + TypeScript)
- [x] Configuración de base de datos (PostgreSQL + Prisma)
- [x] Sistema de autenticación básico (email/password)
- [x] CI/CD pipeline con GitHub Actions

### v0.2.0 — Catálogo
> Enero 2025

- [x] CRUD de productos
- [x] Categorías y tags
- [x] Búsqueda básica por nombre
- [x] Imágenes de productos (upload a S3)

### v0.3.0 — Carrito y Checkout
> Enero 2025

- [x] Carrito de compras (persistente)
- [x] Integración con Stripe (pagos)
- [x] Emails transaccionales (orden confirmada)
- [x] Panel de órdenes para el admin

### v0.3.1 — Hotfix
> Enero 2025

- [x] Fix: carrito no persistía al cerrar sesión
- [x] Fix: precio incorrecto con descuentos activos

---

## 🔄 En progreso

### v0.4.0 — Búsqueda avanzada
> En desarrollo — target: Febrero 2025

- [ ] Filtros por categoría, precio, rating *(80% completado)*
- [ ] Búsqueda full-text con Meilisearch *(en progreso)*
- [ ] Sugerencias de búsqueda en tiempo real *(pendiente)*
- [ ] Historial de búsquedas del usuario *(pendiente)*

---

## 📋 Planeado

### v0.5.0 — Reviews y Ratings
> Planeado para después de v0.4.0

- [ ] Sistema de reseñas de productos
- [ ] Rating con estrellas
- [ ] Moderación de reviews para admin
- [ ] Notificación al vendedor de nueva reseña

### v0.6.0 — Dashboard Analytics
> Planeado

- [ ] Métricas de ventas en tiempo real
- [ ] Gráficos de tendencias por producto
- [ ] Export a CSV/Excel
- [ ] Reporte de abandono de carrito

### v1.0.0 — Release público
> Q2 2025

- [ ] Performance audit y optimizaciones
- [ ] Accesibilidad WCAG 2.1 AA
- [ ] Documentación de API pública
- [ ] Multi-idioma (ES/EN)

---

## 🔮 Backlog / Ideas

- [ ] App mobile (React Native)
- [ ] Programa de afiliados
- [ ] Integración con marketplaces (MercadoLibre, etc.)
- [ ] Modo oscuro

---

## 📌 Versionado

Este proyecto sigue SemVer:
- **Major (X):** Breaking changes en la API pública
- **Minor (Y):** Nuevas features retrocompatibles
- **Patch (Z):** Bug fixes y parches de seguridad
