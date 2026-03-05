# Propuesta: separar Licencia (software) de Licencia ANAC (piloto)

**Autor:** Diagnóstico a partir de feedback del dominio  
**Estado:** Propuesta para implementar  
**Prioridad:** Alta (corrige confusión de dominio)

---

## Tu explicación (resumen)

Cuando se “carga una licencia de piloto” no debería generarse un objeto de tipo **Licencia**, porque en el sistema **Licencia** son **licencias de software** (FlightHub, FlytBase, etc.). Las licencias de piloto (ANAC) no tienen nada que ver con las licencias de software. Por eso proponés crear una tabla/entidad aparte, por ejemplo **LicenciaANAC**.

---

## Opinión / acuerdo

**Estoy de acuerdo.** Es una confusión de dominio tener en la misma entidad:

1. **Licencia de software:** producto comprado (FlightHub, FlytBase, etc.), vinculada a una **Compra** (y opcionalmente a un **Dock** que usa esa licencia). Tipo de compra `LICENCIA_SW` en el dominio actual.
2. **Licencia de piloto (ANAC):** certificación del piloto ante la ANAC (u otro organismo), con número, vencimiento, imagen del documento, vinculada al **Usuario (piloto)**. No tiene relación con Compras ni con Docks.

Son dos conceptos distintos:

- Una es un **activo/ítem comprado** (licencia SW).
- La otra es un **derecho/certificación de una persona** (licencia de piloto).

Mezclarlos en una sola tabla obliga a campos opcionales (`compra` o `piloto`), complica consultas y rompe la claridad del modelo. Además, en la doc original (ESTRUCTURA_BD_ENTIDADES.md) **Licencia** está definida explícitamente como “Licencia (software: FlightHub, FlytBase)” con relación a Compra y Dock; el uso “licencia de piloto” se añadió después sobre la misma entidad.

---

## Propuesta concreta

### 1. Mantener **Licencia** solo para licencias de software

- **Tabla:** `licencias` (como ahora).
- **Relaciones:** ManyToOne con **Compra** (opcional). Referenciada por **Dock** (licenciaId) para indicar qué licencia SW usa ese dock.
- **Campos:** id, nombre, numLicencia, compra (FK), fechaCompra, caducidad, version, activo. Opcional: imagen si en el futuro se guarda un PDF/archivo de la licencia SW.
- **Quitar de Licencia:** el campo **piloto** (y, si la imagen solo se usaba para el documento del piloto, quitar **imagen** de Licencia y dejarla solo en LicenciaANAC).

El **LicenciaRestController** global (`/licencias`) sigue siendo el CRUD de **licencias de software** (como ya está hoy, sin piloto).

### 2. Crear **LicenciaANAC** para licencias del piloto

- **Tabla nueva:** `licencias_anac`.
- **Entidad:** `LicenciaANAC` (paquete `com.gestion.qnt.model`).
- **Relación:** ManyToOne con **Usuario** (piloto). Un piloto puede tener varias licencias ANAC (renovaciones, distintas categorías, etc.).
- **Campos sugeridos:**

| Campo          | Tipo      | Notas                                      |
|----------------|-----------|--------------------------------------------|
| id             | Long      | PK                                         |
| piloto         | Usuario   | ManyToOne, NOT NULL                        |
| fechaVencimientoCma | LocalDate | Vencimiento del CMA                   |
| fechaEmision   | LocalDate | Fecha de emisión del certificado/licencia  |
| caducidad      | LocalDate | Vencimiento de la licencia (certificado de idoneidad) |
| imagenCma      | byte[]    | Foto del Certificado Médico Aeronáutico   |
| imagenCertificadoIdoneidad | byte[] | Foto del Certificado de Idoneidad |
| activo         | Boolean   | default true                               |

**No incluir:** `numeroLicencia` (el número que se confundía con licencia es el CUIT de la persona, que va en el usuario u otro lado). **No incluir:** `nombre` (es el nombre del piloto, ya está en Usuario).

- **Repositorio:** `LicenciaANACRepository` con `List<LicenciaANAC> findByPilotoId(Long pilotoId)` (y métodos que hagan falta).
- **Capa de negocio:** `ILicenciaANACBusiness` / `LicenciaANACBusiness` (CRUD y listado por piloto).
- **API mi-perfil:** Los endpoints bajo **GET/POST/PUT/DELETE /mi-perfil/licencias** pasan a usar **LicenciaANAC**. Para imágenes, **dos fotos por licencia ANAC:** foto del CMA y foto del Certificado de Idoneidad (p. ej. PUT/GET `/mi-perfil/licencias/{id}/imagen-cma` y `/mi-perfil/licencias/{id}/imagen-certificado-idoneidad`). La URL base puede mantenerse para no romper el front; el body de crear/actualizar usa fechaVencimientoCma, fechaEmision, caducidad (sin nombre ni numLicencia).

**Además:** Cada **Usuario** debe poder cargar una **foto de perfil**. Añadir en Usuario el campo `imagenPerfil` (byte[]) y exponer en mi-perfil **PUT /mi-perfil/foto-perfil** (subir) y **GET /mi-perfil/foto-perfil** (descargar), accesibles por cualquier usuario autenticado. Incluir en GET /mi-perfil un booleano `tieneFotoPerfil` para que el front muestre avatar o foto.

### 3. Migración de datos (si ya hay datos de “licencias de piloto” en `licencias`)

- Insertar en `licencias_anac` los registros que en `licencias` tengan `piloto_id` no nulo (copiando piloto_id, num_licencia, nombre, fecha_compra, caducidad, version, activo, imagen).
- Luego eliminar de `licencias` la columna `piloto_id` (y si aplica, `imagen`) o dejarlas en desuso y no usarlas en código.

### 4. Documentación y frontend

- Actualizar **ESTRUCTURA_BD_ENTIDADES.md** y **ENTIDADES.md**: dejar **Licencia** solo para software; añadir **LicenciaANAC** como entidad nueva para certificaciones del piloto.
- En **INFORME_BACKEND_PARA_FRONTEND.md**: aclarar que **GET/POST/PUT/DELETE /mi-perfil/licencias** y las imágenes son sobre **licencias ANAC del piloto** (no sobre licencias de software). El contrato de la API (rutas y formato JSON) puede seguir igual; solo cambia el modelo interno.
- El front no debería tener que cambiar URLs ni estructura de respuesta si se mantienen los mismos nombres de campos (numeroLicencia, caducidad, imagen, etc.) en la respuesta.

---

## Resumen

- **Licencia** = licencia de **software** (Compra, Dock). Se mantiene y se limpia de todo lo que sea “piloto”.
- **LicenciaANAC** = licencia del **piloto** (ANAC u otro organismo). Nueva entidad/tabla; mi-perfil trabaja solo con esta.
- Así el dominio queda coherente y las próximas funcionalidades (reportes por piloto, vencimientos ANAC, etc.) se apoyan en un modelo claro.

Si querés, el siguiente paso es bajar esto a un **prompt de tarea** (paso a paso para el programador) con nombres de clases, métodos y cambios exactos en cada archivo.
