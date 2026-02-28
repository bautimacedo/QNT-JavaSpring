# v0.5.0 — Notificaciones de estado de pedido

**VERSIÓN:** v0.5.0  
**SLUG:** order-notifications  
**DEPENDENCIAS:** v0.4.0 (checkout y órdenes)  
**ESTIMACIÓN:** 2h  
**PRIORIDAD:** alta

---

## Origen

> 🗂️ *Este prompt fue generado en una sesión con el cliente el 2025-02-10.*
> *El cliente pidió: "quiero que mis clientes sepan en qué estado está su pedido sin tener que llamarme".*
> *Traducción técnica: sistema de notificaciones por email en eventos clave del ciclo de vida de una orden.*

---

## Descripción

Implementar notificaciones automáticas por email que se disparan en los eventos
clave del ciclo de vida de una orden: confirmación de compra, preparación en proceso,
y pedido despachado.

El cliente actual llama manualmente a sus compradores para informar el estado —
esto automatiza ese proceso y reduce la carga operativa.

---

## PASO 0 — Contexto previo a leer

> ⚠️ Si venís del AGENTE_WORKFLOW.md, saltear este paso.

Leer sin ejecutar nada:
- `packages/api/src/routes/orders.ts` — handlers de órdenes
- `packages/api/src/services/orders.service.ts` — lógica de negocio de órdenes
- `packages/shared/types/order.ts` — tipo `OrderStatus` y estados definidos

---

## PASO 1 — Revisar los estados de orden existentes

Verificar qué valores tiene el enum `OrderStatus` en `packages/shared/types/order.ts`.

Los estados en los que debemos enviar notificación son:
- `CONFIRMED` → email "Tu pedido fue confirmado"
- `PREPARING` → email "Estamos preparando tu pedido"
- `SHIPPED` → email "Tu pedido está en camino"

Si falta algún estado → crearlo antes de continuar.

**Verificación:** el enum tiene al menos estos 3 valores.

---

## PASO 2 — Crear las plantillas de email

En `packages/api/src/emails/`:

```
order-confirmed.tsx
order-preparing.tsx
order-shipped.tsx
```

Cada plantilla debe incluir:
- Nombre del cliente
- Número de orden
- Resumen del pedido (items + total)
- CTA relevante (ej: "Ver tu pedido" con link)

Usar el sistema de emails existente (Resend / el proveedor configurado en el proyecto).

**Verificación:** las 3 plantillas renderizan sin errores.

---

## PASO 3 — Implementar el servicio de notificaciones

Crear `packages/api/src/services/notifications.service.ts`:

```typescript
// Función principal a implementar:
async function notifyOrderStatusChange(
  orderId: string,
  newStatus: OrderStatus
): Promise<void>
```

La función debe:
- Cargar la orden con datos del cliente desde la DB
- Seleccionar la plantilla según el `newStatus`
- Enviar el email solo si el estado es uno de los 3 notificables
- Loggear el resultado (éxito o error) sin lanzar excepción — las notificaciones
  no deben bloquear el flujo principal

**Verificación:** función implementada con manejo de errores.

---

## PASO 4 — Integrar en el handler de actualización de estado

En `packages/api/src/routes/orders.ts`, en el endpoint que actualiza el estado de una orden:

```typescript
// Después de guardar el nuevo estado en DB, llamar:
await notificationsService.notifyOrderStatusChange(orderId, newStatus)
// Sin await si preferís fire-and-forget — documentarlo
```

**Verificación:** al cambiar el estado de una orden, se dispara el email correspondiente.

---

## PASO 5 — Tests

En `packages/api/src/services/__tests__/notifications.service.test.ts`:

- Test: notifica al pasar a `CONFIRMED`
- Test: notifica al pasar a `SHIPPED`
- Test: NO notifica al pasar a estados no notificables (ej: `CANCELLED`)
- Test: no lanza excepción si el servicio de email falla

Mockear el cliente de email en los tests.

**Verificación:** los 4 tests pasan.

---

## PASO N — Commit y tag

```bash
VERSION="v0.5.0"
SLUG="order-notifications"

git add packages/api/src/emails/ \
        packages/api/src/services/notifications.service.ts \
        packages/api/src/routes/orders.ts \
        packages/api/src/services/__tests__/

git commit --no-verify -m "feat(notifications): notificaciones automáticas por email de estado de orden

- Plantillas: order-confirmed, order-preparing, order-shipped
- NotificationsService con manejo de errores no bloqueante
- Integrado en handler de actualización de estado
- Tests: 4 casos cubiertos"

git tag -a "${VERSION}" -m "Release v0.5.0: Notificaciones de estado de pedido"
```

---

## Verificación final

- [ ] Build sin errores
- [ ] Los 4 tests pasan
- [ ] TypeScript sin errores
- [ ] Al cambiar estado a CONFIRMED/PREPARING/SHIPPED → email enviado en staging
- [ ] Estados no notificables → sin email (verificado en tests)
- [ ] Error de email → orden actualizada igual, error loggeado

---

## Notas

- El cliente pidió explícitamente NO notificar cancelaciones por ahora (quiere llamarlos él)
- Si en el futuro se agrega SMS → crear `NotificationChannel` abstracto en este servicio
- Los templates son básicos — el cliente dijo que los va a personalizar después
