# Estructura detallada de Base de Datos y Modelo de Entidades

> Documento de referencia para implementar las clases `@Entity` JPA y repositorios Spring Data en el backend del Sistema de Gestión Operativa (QNT Gestion).  
> Complementa [ENTIDADES.md](./ENTIDADES.md) y [DOMINIO.md](./DOMINIO.md).

---

## 1. Enum de estado

**Clase:** `Estado` (enum en `com.gestion.qnt.domain` o `com.gestion.qnt.model.entity`)

| Valor (Java)     | Descripción en BD / negocio |
|------------------|-----------------------------|
| `STOCK_ACTUAL`   | En oficina/almacén, disponible para enviar |
| `EN_PROCESO`     | En camino al site o en reparación/servicio |
| `STOCK_ACTIVO`   | Desplegado y en uso en el site |
| `EN_DESUSO`      | Retirado definitivamente (baja) |

Uso: Dock, Dron, Bateria, Helice, AntenaRtk, AntenaStarlink.

---

## 2. Entidades: atributos y tipos

Tipos pensados para Java 21 + JPA (PostgreSQL). Claves primarias: `Long id` con `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

**Nota:** No se modela la entidad **Empresa**: el sistema es para una única empresa (Quintana). Los **Site** existen de forma independiente.

---

### 2.1 Site

| Atributo  | Tipo Java | BD / Notas |
|-----------|-----------|------------|
| id        | Long      | PK |
| nombre    | String    | NOT NULL (ej. EFO, Cañadón Amarillo) |

**Relaciones:**  
- **One-to-Many** → `Dock`, `Pozo`.  
- En la entidad: `@OneToMany(mappedBy = "site") private List<Dock> docks;` y `private List<Pozo> pozos;`

---

### 2.2 Dock

| Atributo              | Tipo Java     | BD / Notas |
|-----------------------|---------------|------------|
| id                    | Long          | PK |
| marca                 | String        | nullable |
| modelo                | String        | nullable |
| estado                | Estado        | enum, NOT NULL |
| numeroSerie           | String        | nullable, índice si se busca por serie |
| nombre                | String        | nullable (nombre o ID visible) |
| fechaCompra           | LocalDate     | nullable |
| horasUso              | Integer       | nullable, default 0 |
| **seguroId**          | Long          | FK nullable → Seguro (tabla Seguro; no String) |
| garantia              | String        | nullable |
| ultimoUso             | Instant       | nullable |
| **ultimoMantenimientoId** | Long      | FK nullable → MantenimientoDock (referencia al último mantenimiento) |
| siteId                | Long          | FK NOT NULL → Site |
| dronId                | Long          | FK nullable, UNIQUE → Dron (1 dock ↔ 1 dron, reemplazable) |
| licenciaId            | Long          | FK nullable → Licencia |
| antenaRtkId           | Long          | FK nullable, UNIQUE → AntenaRtk (1 por dock) |
| antenaStarlinkId      | Long          | FK nullable, UNIQUE → AntenaStarlink (1 por dock) |

**Relaciones:**  
- **Many-to-One** → `Site`, `Dron` (opcional), `Licencia`, `AntenaRtk`, `AntenaStarlink`, **Seguro**, **MantenimientoDock** (ultimoMantenimiento).  
- **One-to-Many** → `MantenimientoDock`, `Log` (si Log es por recurso).  
- Al registrar un nuevo MantenimientoDock para este dock, el servicio actualiza `dock.ultimoMantenimiento` a ese registro.

---

### 2.4 Dron

| Atributo            | Tipo Java  | BD / Notas |
|---------------------|------------|------------|
| id                  | Long       | PK |
| marca               | String     | nullable |
| modelo              | String     | nullable |
| estado              | Estado     | NOT NULL |
| numeroSerie         | String     | nullable |
| nombre              | String     | nullable |
| fechaCompra         | LocalDate  | nullable |
| diasParaMantenimiento  | Integer    | nullable |
| **ultimoMantenimientoId** | Long    | FK nullable → MantenimientoDron (referencia al último mantenimiento) |
| **seguroId**          | Long       | FK nullable → Seguro (tabla Seguro; no String) |
| garantia            | String     | nullable |
| licenciaAnac        | String     | nullable |
| cantidadVuelos     | Integer    | nullable, default 0 |
| cantidadMinutosVolados | Integer | nullable, default 0 |
| incidentes         | String     | @Lob o texto largo, nullable |
| ultimoVuelo        | Instant    | nullable |
| dockId             | Long       | FK nullable, UNIQUE → Dock (1 dron ↔ 1 dock) |

**Relaciones:**  
- **Many-to-One** → `Dock`, **MantenimientoDron** (ultimoMantenimiento).  
- **One-to-Many** → `MantenimientoDron`, `Bateria`, `Helice`, `Mision`.  
- Al registrar un nuevo MantenimientoDron para este dron, el servicio actualiza `dron.ultimoMantenimiento` a ese registro.

---

### 2.5 Proveedor

Proveedor de bienes o servicios (aseguradora, vendor de software, combustible, repuestos, etc.). Toda **Compra** está vinculada a un proveedor.

| Atributo     | Tipo Java | BD / Notas |
|--------------|-----------|------------|
| id           | Long      | PK |
| nombre       | String    | NOT NULL (razón social o nombre del proveedor) |
| cuit         | String    | nullable (CUIT/CUIL) |
| contacto     | String    | nullable (persona de contacto) |
| direccion    | String    | nullable |
| telefono     | String    | nullable |
| email        | String    | nullable |
| observaciones| String    | @Lob, nullable |

**Relaciones:**  
- **One-to-Many** → `Compra` (un proveedor tiene muchas compras).

---

### 2.6 TipoCompra (enum)

Tipos de compra para clasificar el gasto. Valores sugeridos:

| Valor (Java)   | Descripción |
|----------------|-------------|
| `LICENCIA_SW`  | Licencia de software (FlightHub, FlytBase, etc.) |
| `REPUESTO`     | Repuestos (baterías, hélices, etc.) |
| `COMBUSTIBLE`  | Combustible (ej. yendo a un site) |
| `VIATICO`      | Viáticos |
| `SEGURO`       | Póliza de seguro |
| `EQUIPO`       | Equipo (dock, dron, antena, etc.) |
| `OTRO`         | Otros gastos |

---

### 2.7 Compra

Registro de toda compra o gasto, vinculada a un **Proveedor**. Incluye: licencia de software, repuestos, combustible yendo a un site, viáticos, seguros, etc.

| Atributo      | Tipo Java   | BD / Notas |
|---------------|-------------|------------|
| id            | Long        | PK |
| proveedorId   | Long        | FK NOT NULL → Proveedor |
| fechaCompra   | LocalDate   | NOT NULL (fecha del gasto) |
| fechaFactura  | LocalDate   | nullable (fecha de la factura si aplica) |
| numeroFactura | String      | nullable |
| importe       | BigDecimal  | NOT NULL |
| moneda        | String      | NOT NULL, default "ARS" |
| tipoCompra    | TipoCompra  | enum, NOT NULL (LICENCIA_SW, REPUESTO, COMBUSTIBLE, etc.) |
| descripcion   | String      | @Lob, nullable |
| siteId        | Long        | FK nullable → Site (ej. combustible cargado yendo a ese site) |
| observaciones | String      | @Lob, nullable |

**Relaciones:**  
- **Many-to-One** → `Proveedor`, `Site` (opcional).  
- **One-to-Many** (inversa): `Licencia` (compraId), `Seguro` (compraId) y en el futuro otros ítems que se vinculen a una compra.

---

### 2.8 Seguro

Póliza de seguro. Dock y Dron referencian un Seguro (no un String). Opcionalmente vinculado a la **Compra** que pagó la póliza.

| Atributo       | Tipo Java  | BD / Notas |
|----------------|------------|------------|
| id             | Long       | PK |
| aseguradora    | String     | NOT NULL (nombre de la aseguradora) |
| numeroPoliza   | String     | nullable |
| vigenciaDesde  | LocalDate  | nullable |
| vigenciaHasta  | LocalDate  | nullable |
| observaciones  | String     | @Lob, nullable |
| compraId       | Long       | FK nullable → Compra (compra que pagó este seguro) |

**Relaciones:**  
- **Many-to-One** → `Compra` (opcional).  
- **One-to-Many** (inversa): `Dock`, `Dron` (cada uno con seguroId FK nullable → Seguro).

---

### 2.9 Licencia (solo software: FlightHub, FlytBase, etc.)

Entidad exclusiva para licencias de software. No incluye certificaciones de piloto (ver LicenciaANAC).

| Atributo      | Tipo Java  | BD / Notas |
|---------------|------------|------------|
| id            | Long       | PK |
| nombre        | String     | NOT NULL (ej. FlightHub 2, FlytBase) |
| numLicencia   | String     | nullable |
| **compraId**  | Long       | FK nullable → Compra (la compra que adquirió esta licencia; tipo LICENCIA_SW) |
| fechaCompra   | LocalDate  | nullable (redundante con Compra; mantener si se usa en UI) |
| caducidad     | LocalDate  | nullable |
| version       | String     | nullable (ej. Enterprise) |
| activo        | Boolean    | NOT NULL, default true |

**Relaciones:**  
- **Many-to-One** → `Compra` (opcional; la compra de esta licencia al proveedor).  
- **One-to-Many** (inversa) desde `Dock`: muchos docks pueden referenciar la misma licencia; en Licencia: `@OneToMany(mappedBy = "licencia") private List<Dock> docks;` (opcional).

---

### 2.10 LicenciaANAC (certificaciones de piloto)

Tabla: `licencias_anac`. Certificaciones ANAC del piloto (CMA, certificado de idoneidad). El nombre y número de licencia del piloto están en **Usuario**, no aquí.

| Atributo                    | Tipo Java  | BD / Notas |
|-----------------------------|------------|------------|
| id                          | Long       | PK |
| **pilotoId**                | Long       | FK NOT NULL → usuarios |
| fechaVencimientoCma         | LocalDate  | nullable |
| fechaEmision                | LocalDate  | nullable |
| caducidad                   | LocalDate  | nullable |
| imagenCma                   | byte[]     | @Lob, nullable |
| imagenCertificadoIdoneidad  | byte[]     | @Lob, nullable |
| activo                      | Boolean    | default true |

**Relaciones:**  
- **Many-to-One** → `Usuario` (piloto).  
- **Nota:** No tiene `nombre` ni `numeroLicencia`; esos datos pertenecen al piloto en Usuario.

---

### 2.11 AntenaRtk

| Atributo     | Tipo Java | BD / Notas |
|--------------|-----------|------------|
| id           | Long      | PK |
| dockId       | Long      | FK NOT NULL, UNIQUE → Dock (1 por dock) |
| marca        | String    | nullable |
| modelo       | String    | nullable |
| estado       | Estado    | NOT NULL |
| numeroSerie  | String    | nullable |
| nombre       | String    | nullable |
| fechaCompra  | LocalDate | nullable |
| ubicacion    | String    | nullable (coordenadas como texto o JSON) |

**Relaciones:**  
- **One-to-One** con `Dock`: lado dueño en AntenaRtk (`dockId`). Dock tiene `@OneToOne(mappedBy = "dock") AntenaRtk antenaRtk` (o por FK en Dock, según diseño).

---

### 2.12 AntenaStarlink

| Atributo         | Tipo Java  | BD / Notas |
|------------------|------------|------------|
| id               | Long       | PK |
| dockId           | Long       | FK NOT NULL, UNIQUE → Dock |
| marca            | String     | nullable |
| modelo           | String     | nullable |
| estado           | Estado     | NOT NULL |
| numeroSerie      | String     | nullable |
| nombre           | String     | nullable |
| fechaCompra      | LocalDate  | nullable |
| **fechaInstalacion** | LocalDate | nullable (cuándo se instaló en el dock) |

**Relaciones:**  
- **One-to-One** con `Dock` (igual que AntenaRtk).

---

### 2.13 Bateria

| Atributo           | Tipo Java   | BD / Notas |
|--------------------|-------------|------------|
| id                 | Long        | PK |
| marca              | String      | nullable |
| modelo             | String      | nullable |
| estado             | Estado      | NOT NULL |
| numeroSerie        | String      | nullable |
| nombre             | String      | nullable |
| fechaCompra        | LocalDate   | nullable |
| diasUso            | Integer     | nullable |
| ciclosCarga        | Integer     | nullable |
| **fechaStockActivo** | LocalDateTime | nullable (cuándo pasó a Stock activo) |
| **fechaEnDesuso**  | LocalDateTime | nullable (cuándo pasó a En desuso) |
| dronId             | Long        | FK nullable → Dron |

**Relaciones:**  
- **Many-to-One** → `Dron`.  
- **Instalación:** tabla `Instalacion` con `bateriaId` nullable para trazabilidad (quién/cuándo instaló en qué dron).

---

### 2.14 Helice

| Atributo           | Tipo Java   | BD / Notas |
|--------------------|-------------|------------|
| id                 | Long        | PK |
| marca              | String      | nullable |
| modelo             | String      | nullable |
| estado             | Estado      | NOT NULL |
| numeroSerie        | String      | nullable |
| nombre             | String      | nullable |
| fechaCompra        | LocalDate   | nullable |
| horasUso           | Integer     | nullable |
| diasUso            | Integer     | nullable |
| **fechaStockActivo** | LocalDateTime | nullable (cuándo pasó a Stock activo) |
| **fechaEnDesuso**  | LocalDateTime | nullable (cuándo pasó a En desuso) |
| dronId             | Long        | FK nullable → Dron |

**Relaciones:**  
- **Many-to-One** → `Dron`.  
- Instalación: `Instalacion` con `heliceId` nullable.

---

### 2.15 MantenimientoDock

| Atributo    | Tipo Java | BD / Notas |
|-------------|-----------|------------|
| id          | Long      | PK |
| dockId      | Long      | FK NOT NULL → Dock |
| usuarioId   | Long      | FK NOT NULL → Usuario |
| fechaMantenimiento | LocalDateTime | NOT NULL |
| observaciones | String   | @Lob, nullable |
| fotos       | String    | rutas o JSON array, nullable |

**Relaciones:**  
- **Many-to-One** → `Dock`, `Usuario`.

---

### 2.16 MantenimientoDron

| Atributo           | Tipo Java       | BD / Notas |
|--------------------|-----------------|------------|
| id                 | Long            | PK |
| dronId             | Long            | FK NOT NULL → Dron |
| usuarioId          | Long            | FK NOT NULL → Usuario |
| fechaMantenimiento | LocalDateTime   | NOT NULL |
| observaciones      | String          | @Lob, nullable |
| fotos              | String          | nullable |
| **bateriaViejaId** | Long            | FK nullable → Bateria (batería retirada en este mantenimiento) |
| **bateriaNuevaId** | Long            | FK nullable → Bateria (batería instalada) |
| **helicesViejas** | List\<Helice\>  | @ManyToMany: hélices retiradas (tabla join mantenimiento_dron_helices_viejas) |
| **helicesNuevas**  | List\<Helice\>  | @ManyToMany: hélices instaladas (tabla join mantenimiento_dron_helices_nuevas) |

**Relaciones:**  
- **Many-to-One** → `Dron`, `Usuario`, `Bateria` (bateriaVieja), `Bateria` (bateriaNueva).  
- **Many-to-Many** → `Helice` (helicesViejas), `Helice` (helicesNuevas); usar tablas de join distintas para no mezclar los dos conjuntos.

---

### 2.17 Instalacion (trazabilidad)

Registro de quién instaló/reemplazó un componente (dock, dron, batería, hélice) y cuándo.

| Atributo     | Tipo Java | BD / Notas |
|--------------|-----------|------------|
| id           | Long      | PK |
| usuarioId    | Long      | FK NOT NULL → Usuario |
| fecha        | LocalDateTime | NOT NULL |
| observaciones| String    | @Lob, nullable |
| fotos        | String    | nullable |
| dockId       | Long      | FK nullable → Dock (solo uno de dock/dron/bateria/helice no nulo por fila) |
| dronId       | Long      | FK nullable → Dron |
| bateriaId    | Long      | FK nullable → Bateria |
| heliceId     | Long      | FK nullable → Helice |

**Relaciones:**  
- **Many-to-One** → `Usuario`, `Dock`, `Dron`, `Bateria`, `Helice` (todas opcionales salvo Usuario).  
- Restricción de negocio: al menos uno de dockId, dronId, bateriaId, heliceId no nulo (validación en servicio o constraint CHECK).

---

### 2.18 Role (tabla de roles)

Entidad para roles del sistema (admin, piloto, mantenimiento, etc.). Permite ampliar después con permisos por rol.

| Atributo | Tipo Java | BD / Notas |
|----------|-----------|------------|
| id       | Long      | PK |
| codigo   | String    | NOT NULL, UNIQUE (ej. ADMIN, PILOTO, MANTENIMIENTO) |
| nombre   | String    | nullable (nombre legible) |

**Relaciones:**  
- **Many-to-Many** con `Usuario`: tabla join `usuario_roles` (usuario_id, role_id). Un usuario tiene N roles; un rol puede estar asignado a N usuarios.

---

### 2.19 Usuario

| Atributo       | Tipo Java   | BD / Notas |
|----------------|-------------|------------|
| id             | Long        | PK |
| nombre         | String      | NOT NULL |
| apellido       | String      | nullable |
| email          | String      | NOT NULL, UNIQUE |
| password       | String      | NOT NULL (hash, no texto plano) |
| **roles**      | List\<Role\> | @ManyToMany con tabla join usuario_roles (FK a Role) |
| dni            | String      | nullable (piloto) |
| cmaVencimiento | LocalDate   | nullable (piloto, 3 años) |
| cmaImagenes    | String      | rutas o JSON, nullable |
| horasVuelo     | Integer     | nullable |
| cantidadVuelos | Integer     | nullable |
| imagenPerfil   | byte[]      | @Lob, nullable (foto de perfil; no serializar en JSON) |

**Relaciones:**  
- **Many-to-Many** → `Role` (tabla `usuario_roles`).  
- **One-to-Many** → `MantenimientoDock`, `MantenimientoDron`, `Instalacion`, `Mision` (como piloto), `LicenciaANAC`.

---

### 2.20 Pozo

| Atributo   | Tipo Java | BD / Notas |
|------------|-----------|------------|
| id         | Long      | PK |
| nombre     | String    | NOT NULL |
| coordenadas| String    | nullable (lat/lon o JSON) |
| siteId     | Long      | FK NOT NULL → Site |

**Relaciones:**  
- **Many-to-One** → `Site`.  
- **One-to-Many** → `Mision` (historial de inspecciones vía misiones).

---

### 2.21 Mision

| Atributo         | Tipo Java     | BD / Notas |
|------------------|---------------|------------|
| id               | Long          | PK |
| nombre           | String        | NOT NULL |
| linkRtsp         | String        | nullable |
| descripcion      | String        | @Lob, nullable |
| pilotoId         | Long          | FK NOT NULL → Usuario |
| dockId           | Long          | FK nullable → Dock |
| dronId           | Long          | FK nullable → Dron |
| pozoId           | Long          | FK nullable → Pozo |
| fechaCreacion    | LocalDateTime | nullable |
| **ultimaEjecucion** | LocalDateTime | nullable (última vez que se ejecutó esta misión) |
| estado           | String / enum | ej. PLANIFICADA, EJECUTADA, CANCELADA (recomendado: enum EstadoMision) |

**Relaciones:**  
- **Many-to-One** → `Usuario` (piloto), `Dock`, `Dron`, `Pozo`.

---

### 2.22 Log (auditoría / trazabilidad)

| Atributo    | Tipo Java | BD / Notas |
|-------------|-----------|------------|
| id          | Long      | PK |
| entidadTipo | String    | NOT NULL (ej. "Dock", "Dron", "Bateria") |
| entidadId   | Long      | NOT NULL |
| timestamp   | Instant   | NOT NULL |
| tipo        | String    | nullable (tipo de evento) |
| detalle     | String    | @Lob, nullable |
| usuarioId   | Long      | FK nullable → Usuario |

**Relaciones:**  
- **Many-to-One** → `Usuario` (opcional).  
- No FK a entidades polimórficas; se usa `entidadTipo` + `entidadId` para referenciar.

---

## 3. Resumen de relaciones (JPA)

| Entidad            | Relaciones salientes (tiene…) | Relaciones entrantes (lo tienen…) |
|--------------------|--------------------------------|-----------------------------------|
| Site               | 1:N Dock, 1:N Pozo, 1:N Compra (siteId opcional) | — |
| **Proveedor**      | 1:N Compra                    | — |
| **Compra**         | N:1 Proveedor, N:1 Site (opcional) | 1:N Licencia (compraId), 1:N Seguro (compraId) |
| **Seguro**         | N:1 Compra (opcional)         | 1:N Dock, 1:N Dron (seguroId) |
| Dock               | N:1 Site; 1:1 Dron; N:1 Licencia, AntenaRtk, AntenaStarlink, **Seguro**, MantenimientoDock (ultimoMantenimiento); 1:N MantenimientoDock | N:1 Site; 1:1 Dron; 1:N MantenimientoDock |
| Dron               | N:1 Dock, **MantenimientoDron** (ultimoMantenimiento); 1:N MantenimientoDron, Bateria, Helice, Mision; N:1 **Seguro** | 1:1 Dock; 1:N Bateria, Helice, Mision |
| Licencia           | N:1 **Compra** (opcional)     | 1:N Dock |
| LicenciaANAC       | N:1 Usuario (piloto)          | — |
| AntenaRtk          | N:1 Dock (dueño FK)           | 1:1 Dock |
| AntenaStarlink     | N:1 Dock (dueño FK)           | 1:1 Dock |
| Bateria            | N:1 Dron                      | 1:N Dron; N:1 MantenimientoDron (bateriaVieja/bateriaNueva) |
| Helice             | N:1 Dron                      | 1:N Dron; N:N MantenimientoDron (helicesViejas/helicesNuevas) |
| MantenimientoDock  | N:1 Dock, N:1 Usuario         | 1:1 Dock (ultimoMantenimiento) |
| MantenimientoDron  | N:1 Dron, N:1 Usuario; N:1 Bateria (vieja), N:1 Bateria (nueva); N:N Helice (viejas/nuevas) | 1:1 Dron (ultimoMantenimiento) |
| Instalacion        | N:1 Usuario; N:1 Dock/Dron/Bateria/Helice (opcionales) | — |
| **Role**           | N:N Usuario                   | N:N Usuario (tabla usuario_roles) |
| Usuario            | N:N Role; 1:N MantenimientoDock, MantenimientoDron, Instalacion, Mision, LicenciaANAC | — |
| Pozo               | 1:N Mision                    | N:1 Site |
| Mision             | N:1 Usuario, Dock, Dron, Pozo | — |
| Log                | N:1 Usuario (opcional)        | — (referencia polimórfica por tipo+id) |

---

## 4. Convenciones para la implementación

- Paquete base entidades: `com.gestion.qnt.domain` o `com.gestion.qnt.model.entity`.  
- Repositorios: `com.gestion.qnt.repository` extendiendo `JpaRepository<Entidad, Long>`.  
- Nombres de tabla: por defecto plural en inglés (ej. `docks`, `drones`, `mantenimiento_dock`) o según convención del equipo (snake_case).  
- Campos de auditoría: opcional agregar `createdAt`, `updatedAt` (Instant) en entidades base (por ejemplo con `@EntityListeners(AuditingEntityListener.class)`).  
- FKs: nombres de columna `site_id`, `dron_id`, `dock_id`, `usuario_id`, etc.

Este documento debe usarse como especificación para generar las clases `@Entity` y los `JpaRepository` en el proyecto Spring Boot.

---

## 5. Otras sugerencias (opcionales)

- **EstadoMision como enum:** En lugar de `String estado` en Mision, crear enum `EstadoMision` (PLANIFICADA, EN_CURSO, EJECUTADA, CANCELADA) para consistencia y validación.
- **Auditoría en entidades clave:** Agregar `createdAt`, `updatedAt` (Instant) y opcionalmente `createdBy` en Dock, Dron, Usuario, Mision mediante `@EntityListeners(AuditingEntityListener.class)` y `@CreatedDate` / `@LastModifiedDate`.
- **Índices:** Definir `@Table(indexes = ...)` o índices en BD para: `Dock.estado`, `Dron.estado`, `Bateria.dronId`, `Mision.pilotoId`, `Mision.ultimaEjecucion`, y `numeroSerie` donde se busque por serie.
- **Constraint único por site:** En `Pozo`, considerar `(site_id, nombre)` o `(site_id, codigo)` único para evitar pozos duplicados por site.
- **Fotos/adjuntos:** A medio plazo, valorar una entidad `Adjunto` (id, entidadTipo, entidadId, ruta, contentType, fecha) en lugar de campos String en MantenimientoDock, MantenimientoDron e Instalacion, para múltiples archivos y metadata.
- **Soft delete:** Si se requiere no borrar físicamente registros (ej. Dron, Usuario), agregar `activo` (Boolean) o `deletedAt` (Instant nullable) y filtrar en repositorios/queries.
- **Bean Validation:** Usar `@NotNull`, `@Size`, `@Email`, `@Past`/`@Future` en entidades y DTOs para validación declarativa antes de persistir.
