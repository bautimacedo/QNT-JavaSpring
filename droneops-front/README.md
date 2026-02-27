# DroneOps Manager ERP — Frontend

SPA en **Vue 3 + Vite** con el mismo diseño que el PHP/Blade (Tailwind v4, Instrument Sans, tema slate/emerald).

## Stack

- Vue 3, Vue Router 4, Axios
- Vite 7
- Tailwind CSS v4

## Estructura

```
src/
  components/   AppLayout.vue
  views/        LoginView.vue, DashboardView.vue
  router/       index.js (rutas y guard de auth)
  services/     api.js (axios + interceptors token / 401)
```

## Cómo ejecutar

```bash
npm install
npm run dev
```

Abre `http://localhost:5173`. Por defecto redirige a `/login`; al iniciar sesión (cualquier email/contraseña) se guarda un token mock y se redirige al dashboard.

## Estado actual (solo front)

- **Login:** formulario visual; no llama al backend. Guarda un token mock y redirige a `/`.
- **Dashboard:** datos mock (3 KPIs + tabla de licencias). Para conectar al backend, en `DashboardView.vue` reemplaza el mock por `api.get('/api/dashboard')` y adapta el JSON si el contrato difiere.
- **API:** `src/services/api.js` tiene base URL desde `VITE_API_URL` e interceptors para `Authorization: Bearer` y redirección a `/login` en 401.

## Variables de entorno

- `.env`: `VITE_API_URL=http://localhost:8080` (usado por axios cuando conectes el backend).

## Checklist visual (informe)

- Fondo `bg-slate-950`, texto `text-slate-100`, fuente Instrument Sans
- Layout: header con título, subtítulo y badge "Panel Operativo"; contenedor `max-w-6xl mx-auto px-6 py-8`
- Dashboard: 3 tarjetas (emerald/amber), tabla "Licencias a renovar" con columnas Piloto / Tipo / Vencimiento / Documento
- Login: card centrada, inputs oscuros, botón emerald, enlace "¿Olvidaste tu contraseña?"
