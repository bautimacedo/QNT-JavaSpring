# Diagnóstico: 403 Forbidden en /compras, /compras/tipos-equipo y /proveedores

**Fecha:** 2026-03-01  
**Síntoma:** Las pantallas de Compras y Proveedores muestran "HTTP 403" aunque el usuario está logueado. En Network se ven preflight OPTIONS con 200; los GET a compras o proveedores devuelven 403.

---

## Qué está pasando en el backend

El backend **recibe** la petición a `GET /api/qnt/v1/compras` (y a `GET .../compras/tipos-equipo`) y responde **403** porque la autorización falla:

- La expresión de seguridad es: `hasRole('ADMIN') or hasRole('USER')`.
- El log del servidor dice: `ExpressionAuthorizationDecision [granted=false, expressionAttribute=hasRole('ADMIN') or hasRole('USER')]`.
- Es decir: el usuario con el que se evalúa la petición **no tiene** la authority `ROLE_ADMIN` ni `ROLE_USER`.

Eso solo puede pasar en uno de estos casos:

1. **No se envía el token** en esa petición: el request llega sin header `Authorization: Bearer <token>`. El backend no asocia ningún usuario (o trata la petición como anónima) y al evaluar el rol da **403**.
2. **Se envía token pero sin roles válidos**: el JWT no tiene el claim `roles` o tiene `[]`, o solo tiene otro rol (p. ej. `ROLE_PILOTO`). Entonces tampoco cumple `hasRole('ADMIN') or hasRole('USER')` y da **403**.

El **OPTIONS (preflight)** devuelve 200 porque CORS está bien configurado; el 403 corresponde al **GET** que se hace después del preflight.

---

## Si el problema es del FRONTEND

Lo más probable es que las llamadas desde **Compras** no estén usando el mismo cliente HTTP que ya envía el token en otras rutas (p. ej. `/auth/me`, `/mi-perfil`).

### Qué revisar en el front

1. **Cliente HTTP usado en Compras**
   - En `compras.js` (o donde se llame a `getCompras` / `getTiposEquipo`): ¿se usa el **mismo** axios (o fetch) que en el resto de la app y que ya lleva el interceptor que pone `Authorization: Bearer <token>`?
   - Si Compras usa **otro** axios o un `fetch` sin headers, esas peticiones **no** llevan token y el backend devuelve 403.

2. **Comprobar en el navegador**
   - DevTools → **Network** → filtrar por **Fetch/XHR**.
   - Localizar el **GET** a `compras` o `compras?…` (no el OPTIONS).
   - En ese GET → **Headers** → **Request Headers**.
   - Debe aparecer: **`Authorization: Bearer eyJ...`** (token largo).
   - Si **no** está ese header → el fallo es del front: hay que usar el cliente que inyecta el token en **todas** las peticiones a `/api/qnt/v1/*`.

### Qué pedirle al equipo/agente de frontend

- **Objetivo:** Que **todas** las peticiones a la API (incluidas `GET /compras` y `GET /compras/tipos-equipo`) envíen el header `Authorization: Bearer <token>`.
- **Cómo:** Usar **una sola** instancia de cliente HTTP (p. ej. axios) con un **interceptor de request** que lea el token (localStorage/sessionStorage/store) y añada `Authorization: Bearer <token>` a cada request.
- **Compras:** Asegurarse de que `compras.js` (y cualquier función que llame a la API de compras) use ese cliente con interceptor y **no** otro axios/fetch sin el header.
- **Documentación backend:** Ver sección 3.2 de `INFORME_BACKEND_PARA_FRONTEND.md` (envío del token en todas las rutas protegidas).

---

## Si el problema es del BACKEND

- El backend ya normaliza los roles con prefijo `ROLE_` (p. ej. `ADMIN` → `ROLE_ADMIN`) al construir el usuario desde el JWT.
- Si aun así el usuario admin recibe 403, lo más probable es que **en esa petición no llegue token** (caso 1 de arriba), es decir, causa en el front.
- Como medida adicional, se puede permitir la **lectura** de compras (GET list y GET tipos-equipo) para **cualquier usuario autenticado** (`isAuthenticated()`), y dejar escritura/eliminación solo para ADMIN/USER. Eso se aplicó en el backend como salvaguarda (ver más abajo).

---

## Resumen para decirle al front

- **Problema:** 403 en `GET /compras` y `GET /compras/tipos-equipo` con usuario logueado como admin.
- **Causa más probable:** Esas peticiones **no llevan** el header `Authorization: Bearer <token>`.
- **Qué hacer:** Usar el **mismo** cliente HTTP que ya envía el token en `/auth/me` y en el resto de la app, también para las llamadas de Compras (desde `compras.js` / `ComprasView.vue`). Comprobar en Network que el GET a `compras` tenga en Request Headers el header `Authorization: Bearer ...`.
- **Backend:** Se relajó la lectura de compras a “cualquier usuario autenticado”; si aun así hay 403, la petición no está llegando con token.

---

## Proveedores (mismo problema y solución)

- **Síntoma:** `GET .../proveedores 403` desde proveedores.js / ProveedoresView.vue.
- **Causa:** Igual que compras: token no enviado o backend sin controller.
- **Backend:** Se creó ProveedorRestController; GET list/id/search y POST/PUT con isAuthenticated(); DELETE con hasRole(ADMIN).
- **Frontend:** Que proveedores.js use el mismo cliente HTTP con header Authorization Bearer.
