# Dominio — QNT Gestion (respuestas de producto)

Documento de referencia con decisiones y reglas de negocio aclaradas. Sirve como base para BLUEPRINT, entidades y flujos.

---

## Empresas y sites

- **Alcance:** Solo se modela **Quintana**. No multi-tenant con otras empresas/operadoras.
- **Responsables por empresa:** Pueden ser tanto **contactos de referencia** como **usuarios del sistema**, según el caso.

---

## Dock y dron

- **Relación dock–dron:** Normalmente **1 dock = 1 dron**. En caso extremo (dron roto) se puede asignar otro dron al mismo dock. El sistema debe permitir ese reemplazo.
- **Antena RTK:** Por ahora asumir **1 antena RTK activa por dock** (escalable si en el futuro se soporta más de una).
- **Antena Starlink:** **Una por dock.**

---

## Stock y estados

- **Estados de stock:**  
  - **Stock actual:** En oficina/almacén, disponible para enviar.  
  - **En proceso / En mantenimiento:** Incluye dos casos:
    1. **En camino al site:** El ítem salió de oficina y está en tránsito hacia el site (ej. una batería que van a instalar). Al llegar al site y quedar en uso → pasa a **Stock activo**.
    2. **En reparación/servicio:** Equipo derivado a mantenimiento (interno o externo, ej. enviado a Buenos Aires).
  - **Stock activo:** Desplegado y en uso en el site.  
  - **En desuso:** Retirado definitivamente (vida útil cumplida, no reparable, etc.).

- **Reservado:** Equivale al estado **En proceso** (ítem reservado / en mantenimiento o en tránsito).

- **Baterías y hélices (baja):** Al dar de baja (En desuso) se debe conservar **historial** (qué dron/dock, fechas). Reportes/estadísticas se definen más adelante.

---

## Mantenimiento

- **Checklists (dock y dron):** Los ítems los **define el equipo** en el sistema (no vienen de un manual externo fijo).
- **Calibración GPS/IMU:** Disparadores conocidos: **cada 2 meses** y **después de una caída**. No hay otros disparadores definidos por ahora.
- **Mantenimiento externo:** No hace falta modelar el tercero (proveedor) por ahora; alcanza con estado **En mantenimiento / En proceso** y observaciones.

---

## Pilotos y usuarios

- **CMA (certificado médico aeronáutico):** Vencimiento a **3 años**. Se deben poder **adjuntar imágenes** del CMA.
- **90 días sin vuelo:** Regla no definida aún (ANAC vs política interna); dejar abierto.
- **Contraseña del piloto:** Se usa porque los pilotos **envían misiones por Telegram** cuando hace falta; para enviar necesitan **su propia contraseña**. La automatización de Telegram con N8N usa **la misma base de datos** que este sistema (usuarios y contraseñas viven en QNT Gestion).

---

## Misiones

- **Origen:** Las misiones se crean **primero en FlightHub/FlytBase** y **después se conectan/registran en este sistema** (piloto, dock, dron, pozo, etc.).
- **Pozo:** Es **entidad del sistema** (nombre, coordenadas, site, historial de inspecciones).

---

## Compras y facturación

- **Alcance inicial:** **Solo registro** de gastos, facturas, combustible, viáticos, seguros, etc. (quién, cuánto, a quién pagar). **No** generación de facturas hacia clientes por ahora.
- **IVA y reportes:** Fase posterior.

---

## Integraciones

- **DJI API:** Da información (ej. batería por dock: ciclos, porcentaje). Por ahora **no implementar** integración directa en el backend. En el futuro, esos datos se actualizarán en la **BD de forma periódica** mediante un **flujo de N8N** (N8N consulta DJI y escribe en esta BD).

---

## Resumen de entidades/conceptos confirmados

| Concepto | Decisión |
|----------|----------|
| Empresa | Solo Quintana |
| Dock ↔ Dron | 1 a 1, con posibilidad de reemplazo de dron |
| Antena RTK | 1 por dock (escalable) |
| Antena Starlink | 1 por dock |
| Estados stock | Stock actual, En proceso/En mantenimiento, Stock activo, En desuso |
| “En mantenimiento” | Incluye: en camino al site + en reparación/servicio |
| Checklist | Definido por el equipo en el sistema |
| CMA | Vencimiento 3 años; adjuntar imágenes |
| Contraseña piloto | Misma BD que N8N; uso en Telegram para envío de misiones |
| Misión | Creada en FH/FlytBase → registrada/vincular aquí |
| Pozo | Entidad (nombre, coords, site, historial) |
| Facturación | Solo registro; sin generación de facturas ni reportes IVA por ahora |
| DJI API | No en backend aún; luego N8N actualizará BD periódicamente con datos DJI |
