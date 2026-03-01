# Cómo probar la API (Usuario, Role, Login)

## Requisitos

- Base de datos PostgreSQL con BD `qnt_spring` (o configurar `DB_URL` en `application.properties`).
- Aplicación arrancada: `mvn -f gestion/pom.xml spring-boot:run`.

## Contraseña del usuario en BD

El login compara la contraseña enviada con el **hash BCrypt** guardado en `usuarios.password`. Si ves "Credenciales incorrectas" con usuario y contraseña correctos, el hash en BD no coincide. Para corregirlo:

1. **Diagnóstico (recomendado):** Llamar al endpoint de desarrollo para ver qué está pasando:
   ```bash
   curl -s "http://localhost:8080/api/qnt/v1/demo/checkpass?email=admin@ejemplo.com&password=admin"
   ```
   La respuesta indica:
   - `usuarioEncontrado`: si existe el usuario con ese email.
   - `passwordCoincide`: si la contraseña que enviaste coincide con el hash guardado.
   - `longitudHashEnBD`: debe ser **60** para un hash BCrypt válido. Si es distinto (ej. 6 o 50), el hash se guardó mal o la columna recortó el valor.
   - `hashEmpiezaCon`: debe ser `$2a$` o `$2b$`. Si es otra cosa, no es BCrypt.

2. **Obtener el hash:** `curl -s "http://localhost:8080/api/qnt/v1/demo/encodepass?password=admin"` (usa la contraseña que quieras). Copiar **todo** el resultado, sin espacios ni saltos de línea.

3. **Actualizar en PostgreSQL:** Conectar a la misma BD que usa la app (`qnt_spring` por defecto) y ejecutar:
   ```sql
   UPDATE usuarios SET password = 'HASH_COPIADO_AQUI' WHERE email = 'admin@ejemplo.com';
   ```
   Si la columna `password` tiene longitud limitada (ej. `varchar(50)`), alterarla antes:
   ```sql
   ALTER TABLE usuarios ALTER COLUMN password TYPE varchar(60);
   ```

4. Volver a probar el login y, si sigue fallando, otra vez el paso 1 para ver `passwordCoincide` y `longitudHashEnBD`.

## Primer usuario admin (BD vacía)

Si no hay usuarios en BD, no se puede hacer login. Opciones:

1. **Insertar por SQL:** Crear un rol y un usuario con password hasheado.
   - Obtener hash: `GET /api/qnt/v1/demo/encodepass?password=miClave` (público).
   - Insertar en tabla `roles`: `codigo = 'ROLE_ADMIN', nombre = 'Administrador'`.
   - Insertar en tabla `usuarios`: `nombre, apellido, email, password` (el hash), `activo = true`.
   - Insertar en `usuario_roles` la relación entre el usuario y el rol.

2. **Crear rol y usuario vía API** (requiere tener ya un token ADMIN): ver pasos 1 y 2 abajo; el primer admin debe crearse por SQL o con un script de datos iniciales.

## Flujo de prueba

**Autenticación en cada request (importante):**
- **Solo en el login** se envían credenciales en el **body** JSON: `{"username":"email@ejemplo.com","password":"clave"}`. No se usa header `Authorization`.
- **En todas las peticiones posteriores** (usuarios, roles, compras, etc.) se envía el **token JWT** en el header:
  - `Authorization: Bearer <token>`
  El `<token>` es el string que devuelve el body del login (200 OK). No se vuelven a enviar username ni password en los headers.
- Los **datos del recurso** (ej. cuerpo de una compra, de un usuario) van en el **body** en formato **JSON**.

### Registro público (sin token)

Cualquiera puede registrarse; el usuario queda con estado **PENDIENTE_APROBACION** y no puede hacer login hasta que un ADMIN lo apruebe.

- **Registro:**  
  `POST /api/qnt/v1/auth/register` — **no** enviar header `Authorization`.  
  Body (JSON):
  ```json
  {
    "nombre": "Juan",
    "apellido": "Piloto",
    "email": "juan@ejemplo.com",
    "password": "miClaveSegura"
  }
  ```
  Respuesta **201 Created** con el usuario creado (sin password en el body).  
  Si el email ya existe → **409 Conflict**.

### Login según estado

- **Login (usuario ACTIVO):**  
  `POST /api/qnt/v1/auth/login` con body JSON (no enviar header `Authorization`):
  ```json
  {"username":"admin@ejemplo.com","password":"miClave"}
  ```
  Respuesta **200** con el token JWT en el cuerpo (text/plain).

- **Login (usuario PENDIENTE_APROBACION):**  
  Mismo request con email/password de un usuario recién registrado y aún no aprobado.  
  Respuesta **403 Forbidden** con mensaje: `Tu cuenta está pendiente de aprobación por un administrador`.

- **Login (usuario DESACTIVADO):**  
  Si un admin desactivó al usuario.  
  Respuesta **403 Forbidden** con mensaje: `Tu cuenta está desactivada`.

### Aprobación por ADMIN

Solo usuarios con rol ADMIN pueden listar pendientes y aprobar.

- **Listar usuarios pendientes de aprobación:**  
  `GET /api/qnt/v1/usuarios/pendientes`  
  Header: `Authorization: Bearer <token>` (admin).  
  Respuesta **200** con la lista de usuarios con `estado == PENDIENTE_APROBACION`.

- **Aprobar usuario (asignar rol y activar):**  
  `PUT /api/qnt/v1/usuarios/{id}/aprobar`  
  Header: `Authorization: Bearer <token>` (admin).  
  Body (JSON):
  ```json
  {"roleCodigo": "ROLE_PILOTO"}
  ```
  Valores típicos: `ROLE_ADMIN`, `ROLE_PILOTO`, `ROLE_USER` (deben existir en la tabla `roles`).  
  Respuesta **200 OK** con el usuario actualizado (estado ACTIVO, con el rol asignado). A partir de ahí el usuario puede hacer login.  
  Si el usuario no está pendiente de aprobación → **400 Bad Request**. Si usuario o rol no existen → **404**.

### Resto del flujo (roles, usuarios, etc.)

1. **Crear un rol** (si no existe):  
   `POST /api/qnt/v1/roles` con header `Authorization: Bearer <token>` y body:
   ```json
   {"codigo":"ROLE_ADMIN","nombre":"Administrador"}
   ```
   (La primera vez necesitas un usuario admin ya existente en BD; si no, usar SQL como arriba.)

2. **Crear usuario (como ADMIN):**  
   `POST /api/qnt/v1/usuarios` con header `Authorization: Bearer <token>` y body:
   ```json
   {"nombre":"Admin","apellido":"Sistema","email":"admin@ejemplo.com","password":"miClave","roleCodigos":["ROLE_ADMIN"]}
   ```
   El backend hashea la contraseña; no se devuelve en la respuesta.

3. **Login:**  
   `POST /api/qnt/v1/auth/login` con body JSON (no enviar header `Authorization`; si envías un token viejo o inválido puede devolver **403**):
   ```json
   {"username":"admin@ejemplo.com","password":"miClave"}
   ```
   O con form/query: `username=admin@ejemplo.com` y `password=miClave`.  
   Respuesta 200 con el token JWT en el cuerpo (text/plain).

4. **Usuario actual:**  
   `GET /api/qnt/v1/auth/me` con header `Authorization: Bearer <token>`.  
   Respuesta 200 con el principal (id, email, authorities).

5. **Listar usuarios:**  
   `GET /api/qnt/v1/usuarios` con header `Authorization: Bearer <token>`.  
   Respuesta 200 con la lista.

6. **Cambiar contraseña:**  
   `POST /api/qnt/v1/usuarios/change-password` con body:
   ```json
   {"email":"admin@ejemplo.com","oldPassword":"miClave","newPassword":"nuevaClave"}
   ```

7. **Desactivar / activar usuario:**  
   `PUT /api/qnt/v1/usuarios/disable?email=admin@ejemplo.com`  
   `PUT /api/qnt/v1/usuarios/enable?email=admin@ejemplo.com`

8. **Asignar / quitar rol:**  
   `PUT /api/qnt/v1/usuarios/assign-role` con body `{"email":"juan@ejemplo.com","roleCodigo":"ROLE_USER"}`  
   `PUT /api/qnt/v1/usuarios/remove-role` con body `{"email":"juan@ejemplo.com","roleCodigo":"ROLE_USER"}`

## Compras

Todos los endpoints de compras requieren el header **`Authorization: Bearer <token>`** (el mismo que devuelve el login). Sin token responderán **401**.

- **Listar:**  
  `GET /api/qnt/v1/compras`  
  Header: `Authorization: Bearer <token>`

- **Obtener por id:**  
  `GET /api/qnt/v1/compras/{id}`  
  Header: `Authorization: Bearer <token>`

- **Crear:**  
  `POST /api/qnt/v1/compras`  
  Header: `Authorization: Bearer <token>`  
  Body (JSON), ejemplo:
  ```json
  {
    "proveedorId": 1,
    "fechaCompra": "2025-03-01",
    "fechaFactura": "2025-03-02",
    "numeroFactura": "F-001",
    "importe": 15000.50,
    "moneda": "ARS",
    "tipoCompra": "EQUIPO",
    "descripcion": "Notebook de trabajo",
    "siteId": 1,
    "observaciones": "Entrega en oficina"
  }
  ```
  **Proveedor:** se puede enviar **`proveedorId`** (id de un proveedor ya existente) o **`proveedorNombre`** (nombre del proveedor). Si se envía `proveedorNombre` y no existe un proveedor con ese nombre, se crea automáticamente y se asocia a la compra. Hay que enviar al menos uno de los dos.
  Respuesta **201 Created** con la compra creada en el body. Si se usa `proveedorId` y no existe → **404**. Si `siteId` no existe → **404**.

- **Editar:**  
  `PUT /api/qnt/v1/compras/{id}`  
  Header: `Authorization: Bearer <token>`  
  Body (mismo formato que crear; el `id` va en la URL):
  ```json
  {
    "proveedorId": 1,
    "fechaCompra": "2025-03-01",
    "importe": 18000.00,
    "tipoCompra": "EQUIPO",
    "moneda": "ARS"
  }
  ```
  Respuesta **200 OK** con la compra actualizada. Si la compra no existe → **404**.

- **Eliminar:**  
  `DELETE /api/qnt/v1/compras/{id}`  
  Header: `Authorization: Bearer <token>`  
  Respuesta **204 No Content**. Si la compra no existe → **404**.

Valores válidos de `tipoCompra`: `LICENCIA_SW`, `REPUESTO`, `COMBUSTIBLE`, `VIATICO`, `SEGURO`, `EQUIPO`, `OTRO`.

### Imagen de la factura

- **Subir imagen:**  
  `PUT /api/qnt/v1/compras/{id}/imagen`  
  Header: `Authorization: Bearer <token>`  
  Body: **multipart/form-data** con un campo de tipo archivo llamado `file` (por ejemplo un JPG o PNG).  
  Ejemplo con curl (reemplazar `1` por el id de la compra y `factura.jpg` por la ruta del archivo):
  ```bash
  curl -X PUT "http://localhost:8080/api/qnt/v1/compras/1/imagen" \
    -H "Authorization: Bearer <TOKEN>" \
    -F "file=@factura.jpg"
  ```
  Respuesta **200 OK** si la compra existe. Tamaño máximo por defecto: 10 MB (`spring.servlet.multipart.max-file-size`).

- **Obtener imagen:**  
  `GET /api/qnt/v1/compras/{id}/imagen`  
  Header: `Authorization: Bearer <token>`  
  Respuesta: cuerpo binario de la imagen con `Content-Type: application/octet-stream`. **404** si la compra no existe o no tiene imagen asociada.

- La respuesta de `GET /api/qnt/v1/compras/{id}` (compra completa) **no** incluye el campo de imagen en JSON para evitar payloads grandes.

## Seguros

Todos los endpoints de seguros requieren el header **`Authorization: Bearer <token>`**. Sin token → **401**.

- **Listar:**  
  `GET /api/qnt/v1/seguros`  
  Header: `Authorization: Bearer <token>`

- **Obtener por id:**  
  `GET /api/qnt/v1/seguros/{id}`  
  Header: `Authorization: Bearer <token>`

- **Crear:**  
  `POST /api/qnt/v1/seguros`  
  Header: `Authorization: Bearer <token>`  
  Body (JSON), ejemplo:
  ```json
  {
    "aseguradora": "La Caja Seguros",
    "numeroPoliza": "POL-2025-001",
    "vigenciaDesde": "2025-01-01",
    "vigenciaHasta": "2026-01-01",
    "observaciones": "Cobertura total",
    "compraId": 1
  }
  ```
  Respuesta **201 Created** con el seguro creado. Si `compraId` no existe → **404**.

- **Actualizar:**  
  `PUT /api/qnt/v1/seguros/{id}`  
  Header: `Authorization: Bearer <token>`  
  Body (mismo formato; `id` en la URL):
  ```json
  {
    "aseguradora": "La Caja Seguros",
    "numeroPoliza": "POL-2025-001",
    "vigenciaDesde": "2025-01-01",
    "vigenciaHasta": "2026-06-01",
    "observaciones": "Renovado",
    "compraId": null
  }
  ```
  Respuesta **200 OK**. Si el seguro o la compra no existen → **404**.

- **Eliminar:**  
  `DELETE /api/qnt/v1/seguros/{id}`  
  Header: `Authorization: Bearer <token>`  
  Respuesta **204 No Content**. Solo rol ADMIN.

## Licencias

Todos los endpoints de licencias requieren el header **`Authorization: Bearer <token>`**. Sin token → **401**.

- **Listar:**  
  `GET /api/qnt/v1/licencias`  
  Header: `Authorization: Bearer <token>`

- **Obtener por id:**  
  `GET /api/qnt/v1/licencias/{id}`  
  Header: `Authorization: Bearer <token>`

- **Crear:**  
  `POST /api/qnt/v1/licencias`  
  Header: `Authorization: Bearer <token>`  
  Body (JSON), ejemplo:
  ```json
  {
    "nombre": "Windows 11 Pro",
    "numLicencia": "XXXXX-XXXXX-XXXXX",
    "compraId": 1,
    "fechaCompra": "2025-01-15",
    "caducidad": "2026-01-15",
    "version": "11",
    "activo": true
  }
  ```
  Respuesta **201 Created**. Si `compraId` no existe → **404**. Si no se envía `activo`, por defecto es `true`.

- **Actualizar:**  
  `PUT /api/qnt/v1/licencias/{id}`  
  Header: `Authorization: Bearer <token>`  
  Body (mismo formato; `id` en la URL):
  ```json
  {
    "nombre": "Windows 11 Pro",
    "numLicencia": "XXXXX-XXXXX-XXXXX",
    "compraId": null,
    "fechaCompra": "2025-01-15",
    "caducidad": "2027-01-15",
    "version": "11",
    "activo": false
  }
  ```
  Respuesta **200 OK**. Si la licencia o la compra no existen → **404**.

- **Eliminar:**  
  `DELETE /api/qnt/v1/licencias/{id}`  
  Header: `Authorization: Bearer <token>`  
  Respuesta **204 No Content**. Solo rol ADMIN.

## Mi perfil

Cualquier **usuario autenticado** puede ver y actualizar su perfil (datos personales y cambio de contraseña). Los usuarios con rol **PILOTO** o **ADMIN** además pueden gestionar el **CMA** (Certificado Médico Aeronáutico) y sus **licencias ANAC**. Todas las rutas requieren header **`Authorization: Bearer <token>`** y operan siempre sobre el usuario actual (no se puede editar a otro).

### Configuración del perfil (cualquier usuario autenticado)

- **Ver mi perfil:**  
  `GET /api/qnt/v1/mi-perfil`  
  Respuesta **200** con: `usuario` (sin password), `tieneImagenCma`, y si es PILOTO/ADMIN una lista `licencias` (id, nombre, numLicencia, caducidad).

- **Actualizar datos (nombre, apellido, DNI):**  
  `PUT /api/qnt/v1/mi-perfil`  
  Body (JSON), todos opcionales; solo los enviados se actualizan:
  ```json
  {"nombre":"Juan","apellido":"Pérez","dni":"12345678"}
  ```

- **Cambiar mi contraseña:**  
  `PUT /api/qnt/v1/mi-perfil/cambio-password`  
  Body (JSON):
  ```json
  {"oldPassword":"claveActual","newPassword":"claveNueva"}
  ```
  Respuesta **200** si es correcta. **400** si la contraseña actual no coincide.

### CMA (solo PILOTO o ADMIN)

- **Ver CMA (vencimiento y si tiene imagen):**  
  `GET /api/qnt/v1/mi-perfil/cma`  
  Respuesta: `{"vencimiento":"2026-05-01","tieneImagen":true}`

- **Actualizar vencimiento CMA:**  
  `PUT /api/qnt/v1/mi-perfil/cma`  
  Body: `{"vencimiento":"2026-05-01"}` (fecha ISO)

- **Subir imagen del CMA:**  
  `PUT /api/qnt/v1/mi-perfil/cma/imagen`  
  Body: **multipart/form-data**, parte `file` (archivo imagen). Límite 10 MB.

- **Obtener imagen del CMA:**  
  `GET /api/qnt/v1/mi-perfil/cma/imagen`  
  Respuesta: binario. **404** si no tiene imagen.

### Mis licencias (solo PILOTO o ADMIN)

- **Listar mis licencias:**  
  `GET /api/qnt/v1/mi-perfil/licencias`

- **Crear licencia:**  
  `POST /api/qnt/v1/mi-perfil/licencias`  
  Body (JSON):
  ```json
  {"nombre":"ANAC Comercial","numLicencia":"12345","fechaCompra":"2024-01-01","caducidad":"2026-01-01","version":"1","activo":true}
  ```
  Respuesta **201**. La licencia queda asociada al usuario actual (piloto).

- **Actualizar mi licencia:**  
  `PUT /api/qnt/v1/mi-perfil/licencias/{id}`  
  Body: mismo formato (solo se actualizan las licencias del usuario actual). **404** si el id no existe o no es suya.

- **Eliminar mi licencia:**  
  `DELETE /api/qnt/v1/mi-perfil/licencias/{id}`  
  **204** solo si la licencia es del usuario actual.

- **Subir imagen de una licencia:**  
  `PUT /api/qnt/v1/mi-perfil/licencias/{id}/imagen`  
  Multipart, parte `file`. Solo para licencias propias.

- **Obtener imagen de una licencia:**  
  `GET /api/qnt/v1/mi-perfil/licencias/{id}/imagen`  
  **404** si no tiene imagen o no es suya.

**Rol ROLE_PILOTO:** debe existir en la tabla `roles` (ej. `codigo = 'ROLE_PILOTO', nombre = 'Piloto'`) para que un usuario pueda usar los endpoints de CMA y licencias. Si no existe, crearlo vía API de roles (como ADMIN) o por SQL.

## Rutas protegidas

- `/api/qnt/v1/usuarios/**`, `/api/qnt/v1/roles/**`, `/api/qnt/v1/compras/**`, `/api/qnt/v1/seguros/**`, `/api/qnt/v1/licencias/**` y **`/api/qnt/v1/mi-perfil/**`** requieren autenticación (JWT).
- La mayoría de endpoints exigen rol ADMIN (`@PreAuthorize("hasRole('ADMIN')")`).
- Sin token, las peticiones a estas rutas devuelven **401**.
- **Si el login devuelve 403:** quita el header `Authorization` (y cualquier Bearer token) de la petición de login en Postman; esa ruta es pública y no debe llevar token.

## Migración: columna estado (usuarios existentes)

Si la tabla `usuarios` ya tiene datos antes de usar el flujo registro/aprobación, hay que agregar la columna `estado` con valor por defecto para que los usuarios actuales sigan pudiendo hacer login (se consideran ACTIVO). Con `spring.jpa.hibernate.ddl-auto=update`, Hibernate puede crear la columna; si la BD ya existía sin esa columna, ejecutar en PostgreSQL:

```sql
ALTER TABLE usuarios ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO';
```

Si la columna ya existe pero sin default y hay filas con NULL, actualizar: `UPDATE usuarios SET estado = 'ACTIVO' WHERE estado IS NULL;`
