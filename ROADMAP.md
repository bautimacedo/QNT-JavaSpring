# Roadmap — QNT Gestión

> Estado actual: **v0.6.0 en cola** — prompts pendientes desde v0.1.0; nuevas versiones v0.7.0–v0.9.0 agregadas en esta sesión.

---

## ✅ Completado

*(Ninguna versión cerrada aún; los prompts están en agent-bootstrap/prompts/pendientes/.)*

---

## 📋 En cola (pendientes)

Orden sugerido de ejecución (respetar dependencias):

| Versión | Nombre | Descripción breve |
|---------|--------|-------------------|
| v0.1.0 | entity-repositories | Entidades y repositorios JPA |
| v0.2.0 | model-entities-only | Modelo de entidades |
| v0.3.0 | business-interfaces-and-classes | Interfaces y clases de negocio |
| v0.4.0 | security-jwt | Seguridad y JWT |
| v0.4.1 | fix-security-circular-reference | Fix referencia circular seguridad |
| v0.5.0 | controllers-usuario-role | Controllers Usuario y Role |
| v0.6.0 | controller-compras | REST Controller Compras (CRUD) |
| **v0.7.0** | **compra-imagen-factura** | **Compras: imagen/foto de la factura** |
| **v0.8.0** | **controller-seguros** | **REST Controller Seguros (CRUD)** |
| **v0.9.0** | **controller-licencias** | **REST Controller Licencias (CRUD)** |

Archivos en: `agent-bootstrap/prompts/pendientes/vX.Y.Z-<slug>.md`.

---

## 🔮 Backlog / Ideas

- Controllers para el resto de entidades (Proveedor, Site, Dock, Dron, etc.).
- Búsquedas y filtros en listados (compras por fecha, seguros por vigencia, etc.).

---

## 📌 Notas

- Las versiones v0.7.0, v0.8.0 y v0.9.0 se generaron en sesión de planificación (Agente PM).
- v0.7.0 depende de v0.6.0 (controller compras). v0.8.0 y v0.9.0 dependen de v0.4.0 (JWT).
- Para ejecutar: usar Agente Programador con el prompt correspondiente; al finalizar, Agente Tester para verificación.
