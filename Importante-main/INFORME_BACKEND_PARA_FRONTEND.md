# Informe Backend para Frontend — QNT Gestión

**Proyecto:** QNT-Gestion-Spring (backend)  
**Destinatario:** Equipo/Agentes de frontend  
**Objetivo:** Mantener alineación entre backend y frontend: contratos de API, modelos de datos, autenticación y convenciones.

---

## 1. Resumen ejecutivo

Este documento describe el **contrato del backend** para que el proyecto frontend pueda consumir la API REST de forma coherente. Incluye: base URL, autenticación JWT, listado de recursos y endpoints, formas de los request/response, códigos HTTP, roles, enums y recomendaciones de uso.

**Stack backend:** Spring Boot 3.x, Java 17+, Spring Security (JWT), JPA/Hibernate, PostgreSQL.

**Última actualización del informe:** v0.20.0 (vista pilotos con datos completos). Cubre: v0.13.0 — método de pago y usuario de alta en Compras; v0.17.0 — TipoEquipo en Compra; v0.18.0 — estado NO_LLEGO e inventario automático al comprar equipo; v0.19.0 — horasVuelo/cantidadVuelos y restricción de campos piloto en mi-perfil; v0.20.0 — GET /usuarios/pilotos con PilotoResumenResponse. Este documento se actualiza **al finalizar cada tarea** del backend que afecte la API (endpoints, modelos, auth o convenciones).

---

## 2. Base URL y entorno

| Concepto | Valor |
|----------|--------|
| **Prefijo de la API** | `/api/qnt/v1` |
| **Puerto por defecto** | `8080` |
| **URL base completa (local)** | `http://localhost:8080/api/qnt/v1` |

Todas las rutas REST (salvo login y demo) van bajo este prefijo. El frontend debe configurar la base URL por entorno (desarrollo, staging, producción).

---

## 3. Autenticación (JWT)

### 3.1 Rutas públicas (sin token)

- **POST** `/api/qnt/v1/auth/login` — Login. No requiere `Authorization`.
- **POST** `/api/qnt/v1/auth/register` — Registro de nuevo usuario. No requiere `Authorization`. El usuario queda en estado **PENDIENTE_APROBACION** hasta que un ADMIN lo apruebe.
- **GET** `/api/qnt/v1/demo/**` — Solo desarrollo (ej.: codificar contraseña, verificar hash). No usar en producción.
- **GET** `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` — Documentación Swagger (si está habilitada).

El resto de las rutas **requieren autenticación**.

### 3.2 Cómo enviar el token

- **Header (recomendado):**  
  `Authorization: Bearer <token>`
- **Query (alternativo):**  
  `?authtoken=<token>`

El frontend debe guardar el token devuelto por login y enviarlo en **todas** las peticiones a la API (salvo login). Si el token falta o es inválido/expirado, el backend responde **401 Unauthorized**. Si el token es válido pero el usuario no tiene el rol requerido para ese endpoint, el backend responde **403 Forbidden**.

**Importante:** Para **cada** llamada a rutas protegidas (incluidas `/compras`, `/compras/tipos-equipo`, `/proveedores`, `/mi-perfil`, etc.) el cliente debe enviar el header `Authorization: Bearer <token>`. Usar una única instancia de cliente HTTP (p. ej. axios) con un interceptor que añada este header a todas las peticiones; si alguna ruta usa otro cliente o no envía el token, el backend responderá 403.

### 3.3 Login

- **URL:** `POST /api/qnt/v1/auth/login`
- **Content-Type:** `application/json` (o parámetros de formulario)
- **Body (JSON):**
  ```json
  {
    "username": "email@ejemplo.com",
    "password": "contraseña"
  }
  ```
  En el backend, `username` es el **email** del usuario.

- **Respuesta éxito (200):**  
  Cuerpo en **texto plano** con el JWT (string). No es JSON.
- **Respuesta error:**
  - **400** — "Faltan username y password" (texto plano)
  - **401** — "Credenciales incorrectas" / "Contraseña incorrecta" (texto plano)
  - **403** — Cuenta no habilitada para login (texto plano con mensaje para mostrar al usuario):
    - `Tu cuenta está pendiente de aprobación por un administrador` — usuario recién registrado, aún no aprobado.
    - `Tu cuenta está desactivada` — usuario desactivado por un admin.
  - **500** — "Error al autenticar" (texto plano)

El frontend debe almacenar el token (p. ej. memoria o localStorage) y usarlo en el header `Authorization: Bearer <token>`. Ante **403** en login, mostrar el mensaje del cuerpo al usuario (no confundir con “sin permisos”: aquí el usuario no puede acceder hasta ser aprobado o reactivado).

### 3.4 Registro (POST /auth/register)

- **URL:** `POST /api/qnt/v1/auth/register`
- **Content-Type:** `application/json`
- **Headers:** No enviar `Authorization` (ruta pública).
- **Body (JSON):**
  ```json
  {
    "nombre": "Juan",
    "apellido": "Piloto",
    "email": "juan@ejemplo.com",
    "password": "miClaveSegura"
  }
  ```
  - **nombre** (string, obligatorio).
  - **apellido** (string, opcional).
  - **email** (string, obligatorio, debe ser válido).
  - **password** (string, obligatorio, mínimo 6 caracteres).

- **Respuesta éxito (201):** JSON con el usuario creado (sin campo `password`). El usuario tiene `estado: "PENDIENTE_APROBACION"` y `activo: false`; no puede hacer login hasta que un ADMIN lo apruebe.
- **Respuesta error:**
  - **400** — Errores de validación (campos obligatorios, email inválido, password corta). Cuerpo puede ser objeto de validación o mensaje.
  - **409** — "Ya existe un Usuario con email ..." (email duplicado).
  - **500** — Error interno.

Tras un registro exitoso, el frontend puede mostrar un mensaje del tipo "Cuenta creada. Un administrador debe aprobar tu acceso" y redirigir a login (si el usuario intenta entrar antes de ser aprobado, recibirá 403 con el mensaje de pendiente de aprobación).

### 3.5 Usuario actual (/auth/me)

- **URL:** `GET /api/qnt/v1/auth/me`
- **Headers:** `Authorization: Bearer <token>`
- **Respuesta éxito (200):** JSON con el principal, **siempre** con este formato (el backend devuelve un DTO controlado):
  ```json
  {
    "id": 1,
    "email": "admin@ejemplo.com",
    "username": "admin@ejemplo.com",
    "authorities": ["ROLE_ADMIN"]
  }
  ```
  - `id`: Long, ID del usuario.
  - `email` / `username`: mismo valor (email).
  - **`authorities`:** array de **strings** con los roles del usuario (prefijo `ROLE_`). Ejemplos: `["ROLE_ADMIN"]`, `["ROLE_PILOTO"]`, `["ROLE_PILOTO","ROLE_USER"]`. El frontend debe usar este campo para decidir si mostrar rutas como "Perfil Piloto" (comprobar si `authorities` incluye `"ROLE_PILOTO"`).

- **Respuesta error:** **401** sin cuerpo si no hay token o es inválido.

Útil para mostrar “usuario logueado” y decidir permisos en la UI (por rol).

### 3.6 Expiración del token

Por defecto el token expira en **1 hora** (3600000 ms). Configurable en backend con `jwt.expiration-ms`. El frontend puede refrescar el token haciendo login de nuevo; no hay endpoint de refresh en este informe.

---

## 4. CORS

El backend permite CORS para el origen configurado (`app.cors.allowed-origins`, por defecto `*`). Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS, PATCH. Headers permitidos: todos. El header `Authorization` está expuesto para que el cliente pueda leerlo si lo necesita.

En producción conviene restringir `allowed-origins` al dominio del frontend.

---

## 5. Recursos y endpoints

Resumen por recurso. Todos bajo `GET/POST/PUT/DELETE` según la tabla; **todos requieren autenticación** salvo los indicados en el apartado 3.1.

### 5.1 Auth

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/qnt/v1/auth/login` | Login (body JSON o params) | No |
| POST | `/api/qnt/v1/auth/register` | Registro (usuario queda pendiente de aprobación) | No |
| GET | `/api/qnt/v1/auth/me` | Usuario actual (principal) | Sí |

### 5.2 Usuarios

Base: `/api/qnt/v1/usuarios`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/usuarios` | Listar todos | ADMIN |
| GET | `/usuarios/pendientes` | Listar usuarios pendientes de aprobación (estado PENDIENTE_APROBACION) | ADMIN |
| GET | `/usuarios/pilotos` | Listar pilotos con datos completos (PilotoResumenResponse) | ADMIN |
| GET | `/usuarios/search?email=` | Buscar por email | ADMIN |
| POST | `/usuarios` | Crear usuario | ADMIN |
| PUT | `/usuarios/{id}` | Actualizar usuario | ADMIN |
| PUT | `/usuarios/{id}/aprobar` | Aprobar usuario pendiente (asignar rol y activar; body: AprobarUsuarioRequest) | ADMIN |
| POST | `/usuarios/change-password` | Cambiar contraseña | ADMIN, USER |
| PUT | `/usuarios/disable?email=` | Desactivar usuario | ADMIN |
| PUT | `/usuarios/enable?email=` | Activar usuario | ADMIN |
| PUT | `/usuarios/assign-role` | Asignar rol a usuario | ADMIN |
| PUT | `/usuarios/remove-role` | Quitar rol a usuario | ADMIN |

#### 5.2.1 Respuesta de GET /usuarios/pilotos (PilotoResumenResponse) — v0.20.0

Cada objeto en el array:

| Campo | Tipo | Notas |
|-------|------|-------|
| id | number | |
| nombre | string | |
| apellido | string \| null | |
| email | string | |
| horasVuelo | number \| null | |
| cantidadVuelos | number \| null | |
| cmaVencimiento | string (date) \| null | ISO-8601 (yyyy-MM-dd) |
| estado | string | EstadoUsuario: PENDIENTE_APROBACION, ACTIVO, DESACTIVADO |
| tieneFotoPerfil | boolean | |
| licencias | LicenciaANACResumen[] | Licencias ANAC del piloto (vacío si no tiene) |

Cada objeto en `licencias`:

| Campo | Tipo |
|-------|------|
| id | number |
| fechaVencimientoCma | string (date) \| null |
| fechaEmision | string (date) \| null |
| caducidad | string (date) \| null |
| tieneImagenCma | boolean |
| tieneImagenCertificadoIdoneidad | boolean |
| activo | boolean \| null |

> Sin exposición de password, imagenCma ni imagenPerfil en la respuesta.

### 5.3 Roles

Base: `/api/qnt/v1/roles`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/roles` | Listar todos | ADMIN |
| GET | `/roles/search?codigo=` | Buscar por código | ADMIN |
| POST | `/roles` | Crear rol | ADMIN |
| PUT | `/roles/{id}` | Actualizar rol | ADMIN |

> **Instrucción para el frontend:** `GET /roles` devuelve **todos** los roles de la base de datos (ej. ROLE_ADMIN, ROLE_PILOTO, ROLE_USER). El frontend debe usar esta respuesta para poblar el desplegable "Asignar rol" y mostrar **todos** los roles devueltos, sin filtrar por código ni hardcodear una lista. Así, cuando exista un nuevo rol en la BD, aparecerá automáticamente.

### 5.4 Compras

Base: `/api/qnt/v1/compras`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/compras` | Listar todas (opc. ?tipoCompra=&proveedorId=) | Autenticado |
| GET | `/compras/{id}` | Obtener por ID | ADMIN, USER |
| POST | `/compras` | Crear compra | ADMIN, USER |
| PUT | `/compras/{id}` | Actualizar compra | ADMIN, USER |
| DELETE | `/compras/{id}` | Eliminar compra | ADMIN |
| PUT | `/compras/{id}/imagen` | Subir imagen factura (multipart) | ADMIN, USER |
| GET | `/compras/{id}/imagen` | Descargar imagen factura | ADMIN, USER |
| GET | `/compras/tipos-equipo` | Array de valores del enum TipoEquipo (para dropdown) | Autenticado |

### 5.4.1 Proveedores

Base: `/api/qnt/v1/proveedores`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/proveedores` | Listar todos | Autenticado |
| GET | `/proveedores/search?nombre=` | Buscar por nombre (404 si no existe) | Autenticado |
| GET | `/proveedores/{id}` | Obtener por ID | Autenticado |
| POST | `/proveedores` | Crear proveedor (body: objeto Proveedor) | Autenticado |
| PUT | `/proveedores/{id}` | Actualizar proveedor | Autenticado |
| DELETE | `/proveedores/{id}` | Eliminar (409 si tiene compras asociadas) | ADMIN |

### 5.5 Licencias

Base: `/api/qnt/v1/licencias`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/licencias` | Listar todas | ADMIN, USER |
| GET | `/licencias/{id}` | Obtener por ID | ADMIN, USER |
| POST | `/licencias` | Crear licencia | ADMIN, USER |
| PUT | `/licencias/{id}` | Actualizar licencia | ADMIN, USER |
| DELETE | `/licencias/{id}` | Eliminar licencia | ADMIN |

### 5.6 Seguros

Base: `/api/qnt/v1/seguros`

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/seguros` | Listar todos | ADMIN, USER |
| GET | `/seguros/{id}` | Obtener por ID | ADMIN, USER |
| POST | `/seguros` | Crear seguro | ADMIN, USER |
| PUT | `/seguros/{id}` | Actualizar seguro | ADMIN, USER |
| DELETE | `/seguros/{id}` | Eliminar seguro | ADMIN |

### 5.7 Mi perfil

Base: `/api/qnt/v1/mi-perfil`. Todos los endpoints operan sobre el **usuario autenticado** (no se envía usuarioId). Configuración (perfil y cambio de contraseña) para **cualquier usuario autenticado**; licencias ANAC solo para **PILOTO** o **ADMIN**.

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|--------|
| GET | `/mi-perfil` | Datos del usuario + tieneFotoPerfil + licencias ANAC (si piloto) | Autenticado |
| PUT | `/mi-perfil` | Actualizar nombre, apellido, dni; campos de piloto (passwordMission, horasVuelo, cantidadVuelos) solo para PILOTO/ADMIN | Autenticado |
| PUT | `/mi-perfil/cambio-password` | Cambiar contraseña | Autenticado |
| PUT | `/mi-perfil/foto-perfil` | Subir foto de perfil (multipart, parte `file`) | Autenticado |
| GET | `/mi-perfil/foto-perfil` | Obtener foto de perfil | Autenticado |
| GET | `/mi-perfil/licencias` | Listar mis licencias ANAC | PILOTO, ADMIN |
| POST | `/mi-perfil/licencias` | Crear licencia ANAC (piloto = usuario actual) | PILOTO, ADMIN |
| PUT | `/mi-perfil/licencias/{id}` | Actualizar mi licencia ANAC | PILOTO, ADMIN |
| DELETE | `/mi-perfil/licencias/{id}` | Eliminar mi licencia ANAC | PILOTO, ADMIN |
| PUT | `/mi-perfil/licencias/{id}/imagen-cma` | Subir imagen CMA de licencia (multipart) | PILOTO, ADMIN |
| GET | `/mi-perfil/licencias/{id}/imagen-cma` | Obtener imagen CMA de licencia | PILOTO, ADMIN |
| PUT | `/mi-perfil/licencias/{id}/imagen-certificado-idoneidad` | Subir imagen Cert. Idoneidad (multipart) | PILOTO, ADMIN |
| GET | `/mi-perfil/licencias/{id}/imagen-certificado-idoneidad` | Obtener imagen Cert. Idoneidad | PILOTO, ADMIN |

**Respuesta de GET /mi-perfil:** objeto con `usuario`, `roles` (array en la raíz), `tieneFotoPerfil` (boolean) y `licencias` (array con: id, fechaVencimientoCma, fechaEmision, caducidad, tieneImagenCma, tieneImagenCertificadoIdoneidad, activo) si PILOTO/ADMIN.

---

## 6. Modelos de datos (para el frontend)

Tipos y campos que el backend envía/recibe. Fechas en formato **ISO-8601** (`yyyy-MM-dd` para fechas sin hora). Los listados no están paginados: se devuelve un array completo.

### 6.1 Usuario (entidad devuelta por API)

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | Long |
| nombre | string | |
| apellido | string \| null | |
| email | string | Único, usado como login |
| password | — | No se serializa en JSON (@JsonIgnore) |
| roles | Array de Role | Objetos con id, codigo, nombre |
| estado | string | Enum: `PENDIENTE_APROBACION`, `ACTIVO`, `DESACTIVADO`. Solo si es ACTIVO puede hacer login. |
| dni | string \| null | |
| cmaVencimiento | string (date) \| null | ISO-8601 |
| cmaImagenes | string \| null | |
| imagenPerfil | — | No se serializa; usar GET /mi-perfil/foto-perfil |
| horasVuelo | number \| null | |
| cantidadVuelos | number \| null | |
| passwordMission | string \| null | Opcional, máx. 30 caracteres. Dato del piloto (clave para misiones). Editable en PUT /mi-perfil. |
| activo | boolean | Sincronizado con estado (ACTIVO → true; otros → false) |

### 6.2 Role

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | Long |
| codigo | string | Único (ej. "ADMIN", "USER") |
| nombre | string \| null | |

### 6.3 CreateUsuarioRequest (POST/PUT crear usuario)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| nombre | string | |
| apellido | string | |
| email | string | Sí (para crear) |
| password | string | Sí (para crear) |
| roleIds | number[] | Opcional, lista de IDs de rol |
| roleCodigos | string[] | Opcional, lista de códigos de rol |

Se usa `roleIds` o `roleCodigos` para asignar roles al crear.

### 6.4 RegisterRequest (POST /auth/register)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| nombre | string | Sí |
| apellido | string | No |
| email | string | Sí (formato email válido) |
| password | string | Sí (mín. 6 caracteres) |

### 6.5 AprobarUsuarioRequest (PUT /usuarios/{id}/aprobar)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| roleCodigo | string | Sí. Valores típicos: `ROLE_ADMIN`, `ROLE_PILOTO`, `ROLE_USER` (deben existir en BD). |

El usuario debe estar en estado `PENDIENTE_APROBACION`. Tras aprobar, pasa a `ACTIVO`, se le asigna el rol indicado y puede hacer login.

### 6.6 ActualizarMiPerfilRequest (PUT /mi-perfil)

| Campo | Tipo | Obligatorio | Notas |
|-------|------|-------------|-------|
| nombre | string \| null | No | Solo se actualiza si no es blank. |
| apellido | string \| null | No | |
| dni | string \| null | No | |
| passwordMission | string \| null | No | Solo ROLE_PILOTO o ROLE_ADMIN. Máx. 30 caracteres. Ignorado para otros roles. |
| horasVuelo | number \| null | No | Solo ROLE_PILOTO o ROLE_ADMIN. Ignorado para otros roles. (v0.19.0) |
| cantidadVuelos | number \| null | No | Solo ROLE_PILOTO o ROLE_ADMIN. Ignorado para otros roles. (v0.19.0) |

### 6.7 CambioPasswordMiPerfilRequest (PUT /mi-perfil/cambio-password)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| oldPassword | string | Sí |
| newPassword | string | Sí (mín. 6 caracteres) |

### 6.8 CrearLicenciaMiPerfilRequest (POST /mi-perfil/licencias)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| fechaVencimientoCma | string (date) \| null | No |
| fechaEmision | string (date) \| null | No |
| caducidad | string (date) \| null | No |
| activo | boolean \| null | No |

### 6.9 ActualizarLicenciaMiPerfilRequest (PUT /mi-perfil/licencias/{id})

Mismos campos que CrearLicenciaMiPerfilRequest (fechaVencimientoCma, fechaEmision, caducidad, activo); todos opcionales.

### 6.10 AssignRoleRequest (asignar / quitar rol)

| Campo | Tipo |
|-------|------|
| email | string |
| roleCodigo | string |

### 6.11 ChangePasswordRequest

| Campo | Tipo |
|-------|------|
| email | string |
| oldPassword | string |
| newPassword | string |

### 6.12 Compra (entidad)

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | Long |
| proveedor | Proveedor | Objeto anidado (id, nombre, cuit, ...) |
| fechaCompra | string (date) | ISO-8601 |
| fechaFactura | string (date) \| null | |
| importe | number | BigDecimal (ej. 1234.56) |
| moneda | string | Default "ARS" |
| tipoCompra | string | Enum TipoCompra: ver sección Enums |
| metodoPago | string | Enum MetodoPago: EFECTIVO, TRANSFERENCIA, TARJETA, OTRO |
| companiaTarjeta | string \| null | Solo cuando metodoPago = TARJETA (máx. 50 chars) |
| ultimos4Tarjeta | string \| null | Solo cuando metodoPago = TARJETA; exactamente 4 dígitos |
| tipoEquipo | string \| null | Enum TipoEquipo. **Solo presente cuando `tipoCompra = "EQUIPO"`**. Cuando se crea una compra de tipo EQUIPO, el backend genera automáticamente el ítem en inventario con estado NO_LLEGO. |
| descripcionEquipo | string \| null | Texto libre, máx. 255 chars. Solo aplica cuando `tipoCompra = "EQUIPO"`. |
| descripcion | string \| null | |
| site | Site \| null | Objeto anidado |
| observaciones | string \| null | |
| usuarioAlta | Usuario \| null | Usuario que dio de alta la compra (id, email, nombre, apellido; sin password). Se asigna desde el token en el backend; no enviarlo en el body. |
| imagenFactura | — | No se serializa; usar GET .../imagen |

> **Nota:** El campo `numeroFactura` fue **eliminado** en v0.13.0. No existe en la API ni en la BD.

### 6.13 Proveedor (anidado en Compra)

| Campo | Tipo |
|-------|------|
| id | number |
| nombre | string |
| cuit | string \| null |
| contacto | string \| null |
| direccion | string \| null |
| telefono | string \| null |
| email | string \| null |
| observaciones | string \| null |

Hay CRUD completo en `/api/qnt/v1/proveedores` (ver sección 5.4.1). También se crea/usa desde Compras (proveedorId o proveedorNombre).

### 6.14 Site (anidado en Compra)

| Campo | Tipo |
|-------|------|
| id | number |
| nombre | string |

No hay CRUD dedicado de Site en la API actual; se referencia por `siteId` en CreateCompraRequest.

### 6.15 CreateCompraRequest (POST/PUT compra)

| Campo | Tipo | Obligatorio | Notas |
|-------|------|-------------|-------|
| proveedorId | number \| null | Uno de los dos | ID del proveedor existente |
| proveedorNombre | string \| null | Uno de los dos | Si no hay proveedorId, se crea proveedor con este nombre |
| fechaCompra | string (date) | Sí | ISO-8601 |
| fechaFactura | string (date) \| null | No | |
| importe | number | Sí | > 0 |
| moneda | string \| null | No | Default backend "ARS" |
| tipoCompra | string | Sí | Enum TipoCompra |
| metodoPago | string | **Sí** | Enum MetodoPago: EFECTIVO, TRANSFERENCIA, TARJETA, OTRO |
| companiaTarjeta | string \| null | Cond. | **Obligatorio si metodoPago = "TARJETA"** (no vacío, máx. 50). Ignorado si metodoPago ≠ TARJETA. |
| ultimos4Tarjeta | string \| null | Cond. | **Obligatorio si metodoPago = "TARJETA"** (exactamente 4 dígitos `[0-9]{4}`). Ignorado si metodoPago ≠ TARJETA. |
| tipoEquipo | string \| null | Cond. | **Obligatorio si `tipoCompra = "EQUIPO"`** (enum TipoEquipo). Ignorado en otro caso. Al crear con EQUIPO, el backend crea automáticamente el ítem en inventario con estado NO_LLEGO. |
| descripcionEquipo | string \| null | No | Texto libre, máx. 255 chars. Solo aplica cuando `tipoCompra = "EQUIPO"`. Se usa como nombre inicial del ítem en inventario. |
| descripcion | string \| null | No | |
| siteId | number \| null | No | |
| observaciones | string \| null | No | |

> **Importante:** El usuario de alta **no se envía en el body**. El backend lo toma del token JWT. No enviar `numeroFactura` (campo eliminado en v0.13.0).

### 6.16 Licencia (entidad)

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | Long |
| nombre | string | |
| numLicencia | string \| null | |
| compra | Compra \| null | Objeto anidado (puede ser solo id en algunos casos) |
| fechaCompra | string (date) \| null | |
| caducidad | string (date) \| null | |
| version | string \| null | |
| activo | boolean | Default true |

Solo licencias de software (SW); las licencias ANAC del piloto están en LicenciaANAC.

### 6.17 LicenciaANAC (entidad)

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | PK |
| piloto | object (Usuario) | Lazy; no se serializa directamente |
| fechaVencimientoCma | string (date) \| null | |
| fechaEmision | string (date) \| null | |
| caducidad | string (date) \| null | |
| imagenCma | — | No se serializa; usar GET /mi-perfil/licencias/{id}/imagen-cma |
| imagenCertificadoIdoneidad | — | No se serializa; usar GET /mi-perfil/licencias/{id}/imagen-certificado-idoneidad |
| activo | boolean | |

### 6.18 CreateLicenciaRequest

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| nombre | string | Sí |
| numLicencia | string \| null | |
| compraId | number \| null | |
| fechaCompra | string (date) \| null | |
| caducidad | string (date) \| null | |
| version | string \| null | |
| activo | boolean \| null | |

### 6.19 Seguro (entidad)

| Campo | Tipo | Notas |
|-------|------|--------|
| id | number | Long |
| aseguradora | string | |
| numeroPoliza | string \| null | |
| vigenciaDesde | string (date) \| null | |
| vigenciaHasta | string (date) \| null | |
| observaciones | string \| null | |
| compra | Compra \| null | Anidado |

### 6.20 CreateSeguroRequest

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| aseguradora | string | Sí |
| numeroPoliza | string \| null | |
| vigenciaDesde | string (date) \| null | |
| vigenciaHasta | string (date) \| null | |
| observaciones | string \| null | |
| compraId | number \| null | |

---

## 7. Imagen de factura (Compras)

- **Subir:** `PUT /api/qnt/v1/compras/{id}/imagen`  
  - Content-Type: `multipart/form-data`  
  - Parte del formulario: `file` (archivo binario).  
  - Límite: 10 MB por archivo/petición.
- **Obtener:** `GET /api/qnt/v1/compras/{id}/imagen`  
  - Respuesta: cuerpo binario (application/octet-stream).  
  - 404 si la compra no existe o no tiene imagen.

El frontend puede mostrar la imagen con una URL tipo blob o usando el array buffer de la respuesta.

---

## 8. Códigos HTTP y manejo de errores

| Código | Uso en este backend |
|--------|----------------------|
| 200 | OK, cuerpo con recurso o lista |
| 201 | Created, cuerpo con recurso creado |
| 204 | No Content (p. ej. DELETE correcto, cambio de contraseña) |
| 400 | Bad Request (validación, parámetros faltantes, mensaje en cuerpo) |
| 401 | Unauthorized (sin token, token inválido/expirado o credenciales incorrectas en login) |
| 403 | Forbidden: (1) token válido pero sin permiso para el recurso/acción; (2) **en login**: cuenta pendiente de aprobación o desactivada — el cuerpo lleva el mensaje para mostrar al usuario. |
| 404 | Not Found (recurso no existe; a veces cuerpo vacío, a veces mensaje) |
| 409 | Conflict (ej. email ya existe al crear usuario, código de rol duplicado) |
| 500 | Error interno (mensaje opcional en cuerpo) |

No hay un formato estándar único de error: a veces el cuerpo es un string (mensaje), a veces vacío. El frontend debe contemplar ambos y mostrar un mensaje genérico si no hay cuerpo.

---

## 9. Roles y permisos

- **ROLE_ADMIN:** acceso completo (usuarios, roles, CRUD de compras, licencias, seguros, delete donde aplique, enable/disable usuario, asignar/quitar roles, mi-perfil completo incl. CMA y licencias).
- **ROLE_PILOTO:** puede gestionar **mi-perfil** (datos, cambio de contraseña, **password_mission** —máx. 30 caracteres—, CMA y sus licencias ANAC). Debe poder editar sus datos de piloto: licencia(s) ANAC, certificado CMA y password_mission desde mi-perfil. No puede gestionar otros usuarios ni el CRUD global de licencias/seguros/compras salvo lo expuesto en la API general según configuración.
- **ROLE_USER:** puede listar y gestionar compras, licencias y seguros (CRUD salvo delete de licencia/seguro que es solo ADMIN); puede cambiar su contraseña y ver/editar **mi-perfil** (datos y cambio de contraseña, sin CMA ni licencias propias). No puede gestionar usuarios ni roles.

En las respuestas de `/auth/me`, los roles vienen con prefijo `ROLE_` (ej. `ROLE_ADMIN`). Para comparar en el front: usar el string completo o normalizar quitando el prefijo según convención del front.

---

## 10. Enums (valores válidos)

Usar exactamente estos valores en los JSON (strings).

### EstadoUsuario (usuario)

- `PENDIENTE_APROBACION` — Recién registrado, sin aprobar; no puede hacer login.
- `ACTIVO` — Puede usar la aplicación y hacer login.
- `DESACTIVADO` — Desactivado por admin; no puede hacer login.

### TipoCompra (compras)

- `LICENCIA_SW`
- `REPUESTO`
- `COMBUSTIBLE`
- `VIATICO`
- `SEGURO`
- `EQUIPO`
- `OTRO`

### MetodoPago (compras)

Obligatorio en toda compra (POST y PUT). Valores:

- `EFECTIVO`
- `TRANSFERENCIA`
- `TARJETA` — requiere además `companiaTarjeta` (no vacío) y `ultimos4Tarjeta` (exactamente 4 dígitos). El backend responde **400** si faltan o tienen formato incorrecto.
- `OTRO`

Si `metodoPago ≠ TARJETA`, los campos `companiaTarjeta` y `ultimos4Tarjeta` se ignoran y se guardan como `null`.

### TipoEquipo (compras de tipo EQUIPO)

Solo aplica cuando `tipoCompra = "EQUIPO"`. Se obtiene dinámicamente con `GET /compras/tipos-equipo`.

- `DRON`
- `DOCK`
- `BATERIA`
- `HELICE`
- `ANTENA_RTK`
- `ANTENA_STARLINK`
- `OTRO`

**Flujo del frontend:**
1. El usuario selecciona `tipoCompra` en el formulario de compra.
2. Si `tipoCompra == "EQUIPO"` → mostrar un segundo select con los valores de `GET /compras/tipos-equipo` + campo de texto para `descripcionEquipo`.
3. Si `tipoCompra != "EQUIPO"` → ocultar `tipoEquipo` y `descripcionEquipo`.

### Estado (equipos: Dock, Dron, Batería, etc.)

- `NO_LLEGO` — Comprado, pendiente de llegada física; el admin debe completar los datos al recibirlo. (v0.18.0)
- `STOCK_ACTUAL` — En oficina/almacén, disponible para enviar.
- `EN_PROCESO` — En camino al site o en reparación/servicio.
- `STOCK_ACTIVO` — Desplegado y en uso en el site.
- `EN_DESUSO` — Retirado definitivamente (baja).
- `EN_MANTENIMIENTO` — Retirado temporalmente por reparación.

### EstadoMision (misiones)

- `PLANIFICADA`
- `EN_CURSO`
- `EJECUTADA`
- `CANCELADA`

*(Los controllers actuales exponen sobre todo Compras, Licencias, Seguros, Usuarios y Roles; Estado y EstadoMision pueden usarse en entidades que se expongan en el futuro.)*

---

## 11. Recomendaciones para el frontend

1. **Base URL:** Definir una variable de entorno (o config) por entorno apuntando a `http://<host>:<port>/api/qnt/v1`.
2. **Interceptor HTTP:** Añadir en todas las peticiones (salvo login) el header `Authorization: Bearer <token>`.
3. **401:** Redirigir a login y limpiar token al recibir 401; opcionalmente intentar refresh si se implementa.
4. **403:** Mostrar mensaje de “sin permisos” y no repetir la acción.
5. **Tipado:** Definir interfaces/types en TypeScript (o equivalente) a partir de los modelos de esta sección 6 y de los enums de la sección 10.
6. **Fechas:** Enviar y parsear fechas en formato ISO-8601 (`yyyy-MM-dd` para solo fecha).
7. **Listados:** Los listados no están paginados; si en el futuro se añade paginación, el backend podría cambiar a `Page<>`; el front debería estar preparado para evolucionar.
8. **Imagen de factura:** Usar `multipart/form-data` con parte `file` para subir; para mostrar, usar la respuesta binaria de GET `.../imagen`.
9. **Validación:** Respetar obligatorios y formatos indicados en los DTOs para evitar 400.
10. **Swagger:** Si el backend expone Swagger en `/swagger-ui.html`, usarlo como referencia viva de rutas y esquemas; este informe sigue siendo la fuente de verdad para roles, comportamiento de login/me y convenciones.

---

## 12. Changelog de contratos para el frontend

Historial de cambios que impactan al frontend, ordenado por versión. Cada entrada describe qué cambió y qué debe adaptar el frontend.

---

### v0.13.0 — Método de pago y usuario de alta en Compras

**Qué cambió en el backend:**
- Campo `numeroFactura` **eliminado** de la entidad `Compra` y de `CreateCompraRequest`. No existe en BD ni en API.
- Campos nuevos en `Compra`: `metodoPago`, `companiaTarjeta`, `ultimos4Tarjeta`.
- Campo nuevo en `Compra`: `usuarioAlta` (Usuario que dio de alta; viene del token, no del body).
- Nuevo enum `MetodoPago`: EFECTIVO, TRANSFERENCIA, TARJETA, OTRO.

**Checklist frontend:**
- [ ] Eliminar `numeroFactura` del tipo `Compra` y del formulario de crear/editar compra.
- [ ] Añadir al tipo `Compra`: `metodoPago`, `companiaTarjeta` (null si no TARJETA), `ultimos4Tarjeta` (null si no TARJETA).
- [ ] Añadir `metodoPago` (obligatorio) al payload de POST/PUT compras.
- [ ] En el formulario de compra, mostrar selector `metodoPago`. Si elige TARJETA → mostrar `companiaTarjeta` y `ultimos4Tarjeta` (4 dígitos exactos). Validar antes de enviar.
- [ ] Ante 400 en POST/PUT compras, mostrar el mensaje del cuerpo al usuario.
- [ ] Mostrar `usuarioAlta` en listado/detalle como solo lectura. No enviar en body.

---

### v0.17.0 — TipoEquipo en Compra

**Qué cambió en el backend:**
- Campos nuevos en `Compra` y `CreateCompraRequest`: `tipoEquipo` (enum TipoEquipo), `descripcionEquipo` (string).
- Nuevo endpoint `GET /compras/tipos-equipo` → devuelve array de TipoEquipo.
- Nuevo enum `TipoEquipo`: DRON, DOCK, BATERIA, HELICE, ANTENA_RTK, ANTENA_STARLINK, OTRO.

**Checklist frontend:**
- [ ] Añadir al tipo `Compra`: `tipoEquipo` (string | null), `descripcionEquipo` (string | null).
- [ ] En el formulario de compra: si `tipoCompra == "EQUIPO"` → mostrar selector `tipoEquipo` (poblar con `GET /compras/tipos-equipo`) y campo `descripcionEquipo`. Si no → ocultar ambos.
- [ ] `tipoEquipo` es **obligatorio** cuando `tipoCompra == "EQUIPO"`. Validar antes de enviar.

---

### v0.18.0 — Estado NO_LLEGO e inventario automático al comprar equipo

**Qué cambió en el backend:**
- Nuevo valor en enum `Estado` (equipos): `NO_LLEGO` — equipo comprado, pendiente de llegada física.
- **Comportamiento nuevo:** cuando se registra una compra con `tipoCompra = "EQUIPO"` y `tipoEquipo` ∈ {DRON, BATERIA, HELICE}, el backend crea automáticamente el ítem en inventario con `estado = NO_LLEGO` y `nombre = descripcionEquipo`. Para DOCK, ANTENA_RTK, ANTENA_STARLINK no se crea automáticamente (requieren datos adicionales).

**Checklist frontend:**
- [ ] Añadir `NO_LLEGO` al tipo/enum `Estado` del frontend con descripción "Pendiente de llegada".
- [ ] En vistas de inventario (Drones, Baterías, Hélices): mostrar ítems con `estado = NO_LLEGO` con badge diferenciado (ej. "Pendiente de llegada"). Permitir al admin editarlos para completar datos.
- [ ] En el formulario de nueva compra, cuando `tipoCompra = "EQUIPO"` y `tipoEquipo` ∈ {DRON, BATERIA, HELICE}: informar al usuario que se creará un ítem en inventario automáticamente. El campo `descripcionEquipo` se usará como nombre inicial del ítem.
- [ ] Actualizar filtros/badges de estado de equipos para incluir `NO_LLEGO`.

---

### v0.19.0 — Campos de piloto en PUT /mi-perfil (horasVuelo, cantidadVuelos)

**Qué cambió en el backend:**
- `ActualizarMiPerfilRequest` (PUT /mi-perfil) ahora acepta `horasVuelo` (number | null) y `cantidadVuelos` (number | null).
- **Restricción por rol:** `passwordMission`, `horasVuelo` y `cantidadVuelos` solo se aplican si el usuario tiene ROLE_PILOTO o ROLE_ADMIN. Para ROLE_USER se ignoran silenciosamente.
- Los campos generales (`nombre`, `apellido`, `dni`) siguen siendo editables por cualquier usuario autenticado.

**Checklist frontend:**
- [ ] Actualizar tipo `ActualizarMiPerfilRequest`: añadir `horasVuelo?: number | null` y `cantidadVuelos?: number | null`.
- [ ] En la vista "Perfil Piloto" (ROLE_PILOTO): añadir campos editables `horasVuelo` y `cantidadVuelos` al formulario de edición. Incluirlos en el body de PUT /mi-perfil.
- [ ] Para usuarios con ROLE_USER: no mostrar estos campos como editables en la UI.

---

### v0.20.0 — GET /usuarios/pilotos con datos completos (PilotoResumenResponse)

**Qué cambió en el backend:**
- `GET /usuarios/pilotos` ya **no devuelve** `Usuario` completo. Devuelve un array de `PilotoResumenResponse`.
- El nuevo response incluye: id, nombre, apellido, email, horasVuelo, cantidadVuelos, cmaVencimiento, estado, tieneFotoPerfil, y array `licencias` con resumen de licencias ANAC de cada piloto.
- La entidad `LicenciaANAC` ahora tiene el campo `caducidad` (LocalDate). Aparece en el response de `GET /mi-perfil/licencias` y en `PilotoResumenResponse`.

**Checklist frontend:**
- [ ] Crear/actualizar tipo `PilotoResumenResponse` (ver sección 5.2.1 de este informe para el contrato completo).
- [ ] Actualizar la vista "Pilotos" (ADMIN): consumir el nuevo formato. Mostrar `horasVuelo`, `cantidadVuelos`, `cmaVencimiento`, badge de `estado`, e ícono de foto si `tieneFotoPerfil = true`.
- [ ] Para cada piloto en la vista, mostrar sus licencias ANAC: `fechaVencimientoCma`, `fechaEmision`, `caducidad`, íconos de imagen CMA / certificado de idoneidad.
- [ ] Actualizar tipo de respuesta de `GET /mi-perfil/licencias` para incluir el campo `caducidad` (string date | null).
- [ ] La foto del piloto se obtiene por separado: `GET /mi-perfil/foto-perfil` con el token del piloto.

---

### Checklist genérico al recibir una nueva versión del informe

1. Comparar **sección 6 (Modelos)** con los tipos/interfaces del frontend y ajustar campos añadidos, eliminados o renombrados.
2. Revisar **sección 10 (Enums)** y añadir o actualizar los valores enviados en los JSON.
3. Revisar **sección 5 (Recursos y endpoints)** por nuevos endpoints o cambios de comportamiento.
4. Asegurar que los formularios incluyan todos los campos obligatorios y respeten reglas condicionales.
5. Probar los flujos afectados contra el backend y corregir hasta que coincidan con este informe.

---

## 13. Documentos de referencia en este repo

- **AGENTE_CEO.md / README.md** (Importante-main/AgenteCEO): orquestación y flujo de trabajo de agentes.
- **ROADMAP.md** (raíz): versiones y backlog del producto.

Cuando el backend añada nuevos endpoints o cambie contratos, actualizar este informe antes de hacer merge.

---

*Documento generado para sincronizar backend (QNT-Gestion-Spring) con el proyecto frontend. Versión del informe: 2.0 (cubre v0.13.0 → v0.20.0 inclusive).*
