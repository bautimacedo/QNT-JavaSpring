# Esquema de Pruebas — Bot Telegram + Trazabilidad QNT

> Ejecutar cuando el drone EFO esté operativo. Cada test verifica que el dato fluye correctamente desde Telegram hasta la base de datos.

---

## Pre-condiciones (configurar una sola vez)

- [ ] Bautista tiene `telegram_user_id` seteado → ir a **Mi Perfil** y cargar el ID de Telegram (o via `PATCH /usuarios/{id}/telegram`)
- [ ] Existe al menos una misión EFO en estado `PLANIFICADA` con `webhook_url` y `webhook_bearer` configurados
- [ ] El bot está corriendo en la VPS (`docker compose ps telegram_bot_qnt`)
- [ ] El email listener está activo (ver logs: `docker compose logs -f telegram_bot_qnt | grep -i imap`)

---

## TEST 1 — Lanzamiento de misión desde Telegram

**Pasos:**
1. Abrir el grupo de operaciones en Telegram
2. Mandar `🚀 LANZAR MISION`
3. Seleccionar la misión EFO disponible
4. Confirmar con ✅

**Resultado esperado del bot:** `✅ Misión '{nombre}' lanzada exitosamente`

**Verificar en DB:**
```sql
-- Misión debe estar EN_CURSO con piloto asignado
SELECT id, nombre, estado, piloto_id, fecha_inicio 
FROM misiones WHERE estado = 'EN_CURSO';

-- mision_pendiente debe tener un registro no procesado
SELECT drone_nombre, piloto_nombre, usuario_id, procesado, timestamp_lanzamiento
FROM mision_pendiente 
WHERE procesado = false 
ORDER BY timestamp_lanzamiento DESC LIMIT 1;

-- piloto_id debe corresponder a Bautista
SELECT id, nombre, apellido, telegram_user_id 
FROM usuarios WHERE telegram_user_id IS NOT NULL;
```

**✓ Éxito si:** `misiones.piloto_id` apunta al usuario con el `telegram_user_id` de quien lanzó.

---

## TEST 2 — Registro del DESPEGUE (email FlytBase)

**Pasos:** Esperar el email de "Drone take off" de FlytBase (llega ~30 segundos después del despegue real).  
O simular manualmente:
```bash
curl -X POST https://[DOMINIO]/api/qnt/v1/internal/vuelo-log \
  -H "X-Internal-Secret: n8n-qnt-secret-2026" \
  -H "Content-Type: application/json" \
  -d '{
    "evento": "DESPEGUE",
    "site": "EFO",
    "nombreDron": "EFO-Q1",
    "nombreDock": "EFO-DOCK1",
    "eventId": "test-despegue-001"
  }'
```

**Verificar en DB:**
```sql
-- Registro DESPEGUE con piloto correcto
SELECT id, evento, nombre_dron, piloto, fecha_registro, timestamp_flytbase
FROM vuelos_log 
WHERE event_id = 'test-despegue-001'
   OR (evento = 'DESPEGUE' AND nombre_dron = 'EFO-Q1' AND fecha_registro > NOW() - INTERVAL '5 min');

-- El dron debe estar marcado como fuera del dock
SELECT nombre, drone_en_dock FROM drones WHERE nombre = 'EFO-Q1';
-- drone_en_dock = false ✓
```

**✓ Éxito si:** `vuelos_log.piloto` = nombre de Bautista (tomado de `mision_pendiente`, no del email).

---

## TEST 3 — Registro del ATERRIZAJE y creación del VUELO

**Pasos:** Esperar email de "Drone landed" de FlytBase.  
O simular:
```bash
curl -X POST https://[DOMINIO]/api/qnt/v1/internal/vuelo-log \
  -H "X-Internal-Secret: n8n-qnt-secret-2026" \
  -H "Content-Type: application/json" \
  -d '{
    "evento": "ATERRIZAJE",
    "site": "EFO",
    "nombreDron": "EFO-Q1",
    "nombreDock": "EFO-DOCK1",
    "bateria": 78,
    "eventId": "test-aterrizaje-001"
  }'
```

**Verificar en DB:**
```sql
-- Registro ATERRIZAJE
SELECT id, evento, bateria, fecha_registro 
FROM vuelos_log WHERE event_id = 'test-aterrizaje-001';

-- Registro VUELO creado automáticamente (pairing DESPEGUE + ATERRIZAJE)
SELECT id, evento, nombre_dron, piloto, duracion_minutos, bateria
FROM vuelos_log 
WHERE evento = 'VUELO' AND nombre_dron = 'EFO-Q1'
ORDER BY fecha_registro DESC LIMIT 1;
-- piloto = Bautista, duracion_minutos > 0 ✓

-- mision_pendiente marcada como procesada
SELECT procesado FROM mision_pendiente ORDER BY timestamp_lanzamiento DESC LIMIT 1;
-- procesado = true ✓

-- Drone de vuelta en dock
SELECT nombre, drone_en_dock, ultimo_vuelo FROM drones WHERE nombre = 'EFO-Q1';
-- drone_en_dock = true ✓
```

**✓ Éxito si:** existe un registro `VUELO` con duración calculada y piloto = Bautista.

---

## TEST 4 — Completar misión y acumulación de stats

**Pasos:** Este endpoint es llamado por n8n al detectar el aterrizaje, o manualmente:
```bash
curl -X POST https://[DOMINIO]/api/qnt/v1/internal/misiones/completar-por-drone \
  -H "X-Internal-Secret: n8n-qnt-secret-2026" \
  -H "Content-Type: application/json" \
  -d '{"dronNombre": "EFO-Q1"}'
```

**Verificar en DB:**
```sql
-- Misión volvió a PLANIFICADA (reutilizable)
SELECT id, nombre, estado, fecha_fin 
FROM misiones WHERE id = <ID_MISION>;
-- estado = 'PLANIFICADA', fecha_fin NOT NULL ✓

-- Drone: +1 vuelo, +N minutos volados
SELECT nombre, cantidad_vuelos, cantidad_minutos_volados, ultimo_vuelo 
FROM drones WHERE nombre = 'EFO-Q1';

-- Batería activa: +1 vuelo, +1 ciclo de carga, +N minutos
SELECT nombre, cantidad_vuelos, ciclos_carga, cantidad_minutos_volados
FROM baterias 
WHERE dron_id = (SELECT id FROM drones WHERE nombre = 'EFO-Q1')
  AND estado = 'STOCK_ACTIVO';

-- Hélices activas: +1 vuelo, +N minutos
SELECT nombre, cantidad_vuelos, cantidad_minutos_volados
FROM helices 
WHERE dron_id = (SELECT id FROM drones WHERE nombre = 'EFO-Q1')
  AND estado = 'STOCK_ACTIVO';

-- Piloto (Bautista): +horas de vuelo, +1 vuelo
SELECT nombre, apellido, horas_vuelo, cantidad_vuelos 
FROM usuarios WHERE telegram_user_id = <TELEGRAM_ID_BAUTISTA>;
-- horas_vuelo debe haber aumentado en duracion_minutos/60 ✓
```

**✓ Éxito si:** todos los contadores se incrementaron correctamente con la duración del vuelo.

---

## TEST 5 — Estado del drone en el bot (📊 ESTADO)

**Pasos:**
1. Mandar `📊 ESTADO DRONES` en el grupo de Telegram
2. Verificar que el drone muestra "En base" (después del aterrizaje)

**✓ Éxito si:** el bot responde mostrando EFO-Q1 sin indicar vuelo activo.

---

## TEST 6 — Reporte del día (📈 REPORTE)

**Pasos:**
1. Mandar `📈 REPORTE` en el grupo de Telegram

**Verificar:**
- El reporte muestra el vuelo del día bajo "EFO"
- El total de vuelos y minutos es correcto
- No hay errors en los logs del bot

**✓ Éxito si:** el reporte muestra datos de EFO, no solo CAM.

---

## TEST 7 — Casos de borde

### 7a. Dron ya volando — no debe permitir doble lanzamiento
1. Lanzar una misión (TEST 1)
2. **Sin aterrizar**, intentar lanzar otra misión para EFO-Q1
3. **Esperado:** bot responde `⚠️ El dron 'EFO-Q1' ya tiene un vuelo activo`

**Verificar:**
```sql
-- hayVueloActivo debe devolver true mientras no haya ATERRIZAJE posterior
SELECT COUNT(*) > 0 FROM vuelos_log v
WHERE v.nombre_dron = 'EFO-Q1'
  AND v.evento = 'DESPEGUE'
  AND (v.despegue_fallido IS NULL OR v.despegue_fallido = false)
  AND NOT EXISTS (
    SELECT 1 FROM vuelos_log v2
    WHERE v2.nombre_dron = 'EFO-Q1'
      AND v2.evento = 'ATERRIZAJE'
      AND COALESCE(v2.timestamp_flytbase, v2.fecha_registro) 
        > COALESCE(v.timestamp_flytbase, v.fecha_registro)
  );
```

### 7b. Misión EN_CURSO — no debe ser lanzable de nuevo
1. Verificar que una misión en estado `EN_CURSO` no aparece en la lista del bot
2. **Esperado:** la lista solo muestra misiones `PLANIFICADA`

### 7c. Usuario Telegram no mapeado
1. Un usuario del grupo que NO tiene `telegram_user_id` seteado lanza una misión
2. **Esperado:** la misión se lanza igual, en `misiones.observaciones` debe decir `"Lanzado por Telegram: {nombre}"`

```sql
SELECT observaciones FROM misiones WHERE id = <MISION_ID>;
```

### 7d. Email duplicado — no debe registrar doble
Si FlytBase envía el mismo email dos veces (mismo `eventId`):
```sql
SELECT COUNT(*) FROM vuelos_log WHERE event_id = 'test-despegue-001';
-- debe ser 1, no 2
```

### 7e. Falla de despegue
Si FlytBase envía "Drone take off failure":
```sql
SELECT id, evento, despegue_fallido FROM vuelos_log 
WHERE evento IN ('DESPEGUE', 'FALLA_DESPEGUE') 
ORDER BY fecha_registro DESC LIMIT 3;
-- despegue_fallido = true para las fallas
-- Este DESPEGUE NO debe bloquear futuros lanzamientos
```

---

## TEST 8 — Reporte diario automático (22:00 AR)

**Verificar a las 22:00:**
1. El mensaje llega al grupo de Telegram
2. Incluye vuelos de EFO y CAM separados
3. Muestra alerta si `vuelos_cortos / total > 30%`

**Si no llega:** revisar logs del bot:
```bash
docker compose logs telegram_bot_qnt | grep -i "daily_report\|reporte"
```

---

## Checklist final de trazabilidad completa

| Paso | Verificación | Estado |
|------|-------------|--------|
| Lanzamiento desde Telegram | `misiones.estado = EN_CURSO`, `piloto_id = Bautista` | ⬜ |
| `mision_pendiente` creado | `procesado = false`, `piloto_nombre = Bautista` | ⬜ |
| DESPEGUE recibido por email | `vuelos_log.piloto = Bautista` (de mision_pendiente) | ⬜ |
| `drones.drone_en_dock = false` | Drone marcado como volando | ⬜ |
| ATERRIZAJE recibido | Registro en `vuelos_log` | ⬜ |
| VUELO creado automáticamente | `duracion_minutos > 0`, `piloto = Bautista` | ⬜ |
| `mision_pendiente.procesado = true` | Cerrado correctamente | ⬜ |
| `drones.drone_en_dock = true` | Drone de vuelta en base | ⬜ |
| Misión → `PLANIFICADA` | Reutilizable para el próximo vuelo | ⬜ |
| `drones.cantidad_vuelos += 1` | Stats del drone actualizadas | ⬜ |
| `baterias.ciclos_carga += 1` | Stats de batería actualizadas | ⬜ |
| `helices.cantidad_vuelos += 1` | Stats de hélices actualizadas | ⬜ |
| `usuarios.horas_vuelo += N/60` | Horas acreditadas a Bautista | ⬜ |
