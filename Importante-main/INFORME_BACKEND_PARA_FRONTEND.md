# Informe Backend para Frontend — QNT Gestión

**Proyecto:** QNT-Gestion-Spring (backend)  
**Destinatario:** Equipo/Agentes de frontend  
**Objetivo:** Mantener alineación entre backend y frontend: contratos de API, modelos de datos, autenticación y convenciones.

---

## 1. Resumen ejecutivo

Este documento describe el **contrato del backend** para que el proyecto frontend pueda consumir la API REST de forma coherente. Incluye: base URL, autenticación JWT, listado de recursos y endpoints, formas de los request/response, códigos HTTP, roles, enums y recomendaciones de uso.

**Stack backend:** Spring Boot 3.x, Java 17+, Spring Security (JWT), JPA/Hibernate, PostgreSQL.

**Última actualización del informe:** Tras v0.15.0 (separar Licencia SW de LicenciaANAC, foto de perfil). Este documento se actualiza **al finalizar cada tarea** del backend que afecte la API (endpoints, modelos, auth o convenciones).

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

El frontend debe guardar el token devuelto por login y enviarlo en **todas** las peticiones a la API (salvo login). Si el token falta o es inválido/expirado, el backend responde **401 Unauthorized**.

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
| GET | `/usuarios/pilotos` | Listar usuarios con rol ROLE_PILOTO | ADMIN |
| GET | `/usuarios/search?email=` | Buscar por email | ADMIN |
| POST | `/usuarios` | Crear usuario | ADMIN |
| PUT | `/usuarios/{id}` | Actualizar usuario | ADMIN |
| PUT | `/usuarios/{id}/aprobar` | Aprobar usuario pendiente (asignar rol y activar; body: AprobarUsuarioRequest) | ADMIN |
| POST | `/usuarios/change-password` | Cambiar contraseña | ADMIN, USER |
| PUT | `/usuarios/disable?email=` | Desactivar usuario | ADMIN |
| PUT | `/usuarios/enable?email=` | Activar usuario | ADMIN |
| PUT | `/usuarios/assign-role` | Asignar rol a usuario | ADMIN |
| PUT | `/usuarios/remove-role` | Quitar rol a usuario | ADMIN |

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
| GET | `/compras` | Listar todas | ADMIN, USER |
| GET | `/compras/{id}` | Obtener por ID | ADMIN, USER |
| POST | `/compras` | Crear compra | ADMIN, USER |
| PUT | `/compras/{id}` | Actualizar compra | ADMIN, USER |
| DELETE | `/compras/{id}` | Eliminar compra | ADMIN |
| PUT | `/compras/{id}/imagen` | Subir imagen factura (multipart) | ADMIN, USER |
| GET | `/compras/{id}/imagen` | Descargar imagen factura | ADMIN, USER |

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
| PUT | `/mi-perfil` | Actualizar nombre, apellido, dni, passwordMission | Autenticado |
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

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| nombre | string \| null | No; solo los enviados se actualizan |
| apellido | string \| null | No |
| dni | string \| null | No |
| passwordMission | string \| null | No; máx. 30 caracteres. Dato del piloto (clave para misiones), no es la contraseña de login. |

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
| proveedor | Proveedor | Objeto anidado |
| fechaCompra | string (date) | ISO-8601 |
| fechaFactura | string (date) \| null | |
| numeroFactura | string \| null | |
| importe | number | BigDecimal (ej. 1234.56) |
| moneda | string | Default "ARS" |
| tipoCompra | string | Enum: ver sección Enums |
| descripcion | string \| null | |
| site | Site \| null | Objeto anidado |
| observaciones | string \| null | |
| imagenFactura | — | No se serializa; usar GET .../imagen |

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

No hay CRUD dedicado de Proveedor en la API; se crea/usa desde Compras (proveedorId o proveedorNombre).

### 6.14 Site (anidado en Compra)

| Campo | Tipo |
|-------|------|
| id | number |
| nombre | string |

No hay CRUD dedicado de Site en la API actual; se referencia por `siteId` en CreateCompraRequest.

### 6.15 CreateCompraRequest (POST/PUT compra)

| Campo | Tipo | Obligatorio |
|-------|------|-------------|
| proveedorId | number \| null | Uno de proveedorId o proveedorNombre |
| proveedorNombre | string \| null | Si no hay proveedorId, se crea proveedor con este nombre |
| fechaCompra | string (date) | Sí |
| fechaFactura | string (date) \| null | |
| numeroFactura | string \| null | |
| importe | number | Sí, > 0 |
| moneda | string \| null | Default backend "ARS" |
| tipoCompra | string | Sí (enum TipoCompra) |
| descripcion | string \| null | |
| siteId | number \| null | |
| observaciones | string \| null | |

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

### Estado (equipos: Dock, Dron, Batería, etc.)

- `STOCK_ACTUAL`
- `EN_PROCESO`
- `STOCK_ACTIVO`
- `EN_DESUSO`

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

## 12. Documentos de referencia en este repo

- **AGENTE_CEO.md / README.md** (Importante-main/AgenteCEO): orquestación y flujo de trabajo de agentes (planificación, ejecución, estado del proyecto).
- **ROADMAP.md** (raíz): versiones y backlog del producto; incluye ideas de controllers para Proveedor, Site, Dock, Dron, etc., y filtros/búsquedas.

Cuando el backend añada nuevos endpoints o cambie contratos, conviene actualizar este informe y la documentación del front para mantener la alineación.

---

*Documento generado para sincronizar backend (QNT-Gestion-Spring) con el proyecto frontend. Versión del informe: 1.5 (v0.15.0 — Separar Licencia SW de LicenciaANAC y foto de perfil).*
