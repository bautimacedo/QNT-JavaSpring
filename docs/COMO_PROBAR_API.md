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
  Respuesta **201 Created** con la compra creada en el body. Si `proveedorId` o `siteId` no existen → **404**.

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

## Rutas protegidas

- `/api/qnt/v1/usuarios/**`, `/api/qnt/v1/roles/**` y `/api/qnt/v1/compras/**` requieren autenticación (JWT).
- La mayoría de endpoints exigen rol ADMIN (`@PreAuthorize("hasRole('ADMIN')")`).
- Sin token, las peticiones a estas rutas devuelven **401**.
- **Si el login devuelve 403:** quita el header `Authorization` (y cualquier Bearer token) de la petición de login en Postman; esa ruta es pública y no debe llevar token.
