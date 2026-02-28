# Entidades y modelo de datos — QNT Gestion

Definición de entidades del sistema y sus atributos. Referencia para implementación (JPA/BD) y para [BLUEPRINT.md](../BLUEPRINT.md).

Las reglas de negocio sobre estados, relaciones y flujos están en [DOMINIO.md](./DOMINIO.md).

---

## Estados (enum / valor fijo)

**ESTADO** — Aplica a Dock, Dron, Batería, Hélice, Antena RTK, Antena Starlink, etc.

| Valor              | Descripción |
|--------------------|-------------|
| Stock actual       | En oficina/almacén, disponible para enviar. |
| En proceso         | En camino al site o en reparación/servicio. |
| Stock activo       | Desplegado y en uso en el site. |
| En desuso          | Retirado definitivamente (baja). |

---

## Empresa y Site

**EMPRESA**
- Nombre.
- Responsables: lista de nombres (ej. "Hugo Meriño") — pueden ser contacto y/o usuario del sistema.

**SITE**
- Nombre (ej. EFO, Cañadón Amarillo, Cañadón Seco).
- Empresa (FK).

---

## Instalación (trazabilidad)

**INSTALACIÓN**
- Usuario que realizó la instalación (FK Usuario).
- Fecha.
- Observaciones.
- Fotos (rutas o referencias).

*(Se asocia a Dock, Dron, Batería, Hélice, etc. cuando se instala o reemplaza un componente.)*

---

## Dock

**DOCK**
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.
- Lista de mantenimientos (MantenimientoDock).
- Dron asignado (FK, 1 a 1; puede reemplazarse).
- Instalación (quién/cuándo instaló el dock).
- Horas de uso.
- Site (FK).
- Licencia activa (FK Licencia).
- Antena Starlink (FK, 1 por dock).
- Antena RTK (FK, 1 por dock).
- Seguro, Garantía.
- Último uso (timestamp).
- Logs (lista de logs asociados).

---

## Dron

**DRON**
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.
- Mantenimiento dron: lista (MantenimientoDron), últimas calibraciones GPS, IMU.
- Instalación.
- Días para próximo mantenimiento.
- Dock (FK, 1 a 1).
- Seguro, Garantía.
- Batería(s) asignada(s) (relación con Batería).
- Hélices (relación con Hélice).
- Speakers, Luces (si se modelan como entidades o como atributos/cantidad).
- Misiones (lista de misiones en las que participó).
- Licencia ANAC (si aplica).
- Cantidad de vuelos, Cantidad de minutos volados.
- Incidentes (texto o FK a entidad Incidente si se define).
- Último vuelo (timestamp).
- Cantidad de días para mantenimiento.

---

## Mantenimiento Dock

**MANTENIMIENTO_DOCK**
- Dock (FK).
- Usuario que realizó el mantenimiento (FK Usuario).
- Fecha de mantenimiento.
- Observaciones.
- Fotos.
- *(Checklist: definido en front como guía; no necesariamente como atributos de la entidad.)*

---

## Mantenimiento Dron

**MANTENIMIENTO_DRON**
- Dron (FK).
- Usuario que realizó el mantenimiento (FK Usuario).
- Fecha de mantenimiento.
- Observaciones.
- Fotos.
- *(Checklist: guía en front; calibración GPS/IMU, cambio de hélices/baterías, etc.)*

---

## Licencia

**LICENCIA**
- Nombre (ej. FlightHub 2, FlytBase).
- Fecha de compra, Caducidad.
- Versión (ej. Enterprise).
- Activo (sí/no).

*(Asociada a Dock: licencia activa del dock.)*

---

## Antena RTK

**ANTENA_RTK**
- Dock (FK).
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.
- Ubicación (coordenadas).

---

## Antena Starlink

**ANTENA_STARLINK**
- Dock (FK).
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.

---

## Batería

**BATERÍA**
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.
- Días de uso, Ciclos de carga.
- Instalación (cuándo/dónde se instaló en qué dron; historial si se reemplaza).

---

## Hélice

**HELICE**
- Marca, Modelo.
- Estado (enum ESTADO).
- Número de serie, Nombre o ID.
- Fecha de compra.
- Horas de uso, Días de uso.
- Instalación (trazabilidad: quién/cuándo la puso en qué dron).

---

## Usuario / Piloto

**USUARIO**
- Roles (ej. admin, piloto, mantenimiento).
- Nombre, Apellido.
- Email, Password (usado también por N8N/Telegram para envío de misiones).
- *(Si es piloto:)* CMA (certificado médico aeronáutico): vencimiento 3 años, imágenes adjuntas. DNI. Horas de vuelo, cantidad de vuelos. Licencia ANAC si aplica.

---

## Pozo

**POZO**
- Nombre o ID.
- Coordenadas (ubicación).
- Site (FK).
- Historial de inspecciones (relación con Misiones o registros de inspección).

---

## Misión

**MISION**
- Nombre.
- Link RTSP (opcional).
- Descripción (datos que vienen de FlightHub/FlytBase).
- Piloto asignado (FK Usuario).
- Dock / Dron (FK).
- Pozo (FK, opcional).
- Parámetros adicionales (según integración con FH/FlytBase).
- Fecha/hora (creación, ejecución).
- Estado (ej. planificada, ejecutada, cancelada).

---

## Logs

**LOG**
- Entidad o recurso al que aplica (Dock, Dron, etc.).
- Timestamp.
- Tipo / descripción.
- Detalle (texto o payload).
- *(Puede ser tabla genérica de auditoría o logs específicos por recurso.)*

---

## Compras (fase posterior)

**COMPRA** — Para sistema de compras/facturación (registro de gastos, facturas, combustible, viáticos, seguros). Atributos a definir en fase de compras.

---

## Diagrama de relaciones (resumen)

```
Empresa 1 ──* Site 1 ──* Dock 1 ──1 Dron
                │         │  │
                │         │  ├── Licencia (1)
                │         │  ├── Antena RTK (1)
                │         │  ├── Antena Starlink (1)
                │         │  └── MantenimientoDock (*)
                │         │
                │         └── Dron 1 ──* Batería, * Hélice, * MantenimientoDron
                │
                └── * Pozo ──* Mision ← Usuario (piloto)
```

---

*Documento vivo: ajustar atributos y relaciones según implementación y nuevos requisitos.*
