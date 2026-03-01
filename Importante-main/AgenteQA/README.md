# Agente QA / Tester

Este directorio contiene el **Agente Tester** del pipeline: verificación de calidad (build + tests + opcional smoke API) y reporte estándar. No implementa código; solo ejecuta y reporta.

---

## Archivo principal

| Archivo | Propósito |
|---------|-----------|
| **AGENTE_TESTER.md** | Prompt del agente: pasos de verificación, reporte y reglas. Versión 2.0, lista para pipeline de producción. |

---

## Rol en el pipeline

El flujo secuencial es:

```
Usuario → PM (genera prompt) → Programador (implementa) → Tester (verifica y reporta)
```

- **PM:** al generar un prompt en `pendientes/`, invoca al Programador con README + AGENTE_WORKFLOW + template + @prompt.
- **Programador:** al terminar implementación, commit, tag y merge, invoca al Tester (PASO 6.5 del AGENTE_WORKFLOW).
- **Tester:** ejecuta `./gestion/mvnw -f gestion/pom.xml clean test`, opcionalmente smoke API, y emite reporte **Listo para merge** o **No listo**.

Si el Tester reporta **No listo**, el Programador debe corregir y volver a invocar al Tester antes de dar el checkpoint al usuario.

---

## Cómo invocar al Tester

### Desde el pipeline (automático)

El Agente Programador invoca al Tester al terminar una versión. No hace falta que lo llames vos.

### Manual (solo verificación)

Para ejecutar solo la batería de tests y el reporte:

```
Lee Importante-main/AgenteQA/AGENTE_TESTER.md y ejecutá la verificación.
```

O desde la raíz del repo:

```bash
./gestion/mvnw -f gestion/pom.xml clean test -q
# Opcional si la app está en 8080:
./scripts/smoke-test-api.sh
```

---

## Documentación relacionada

- **Pipeline completo:** [Importante-main/PIPELINE_SECUENCIAL.md](../PIPELINE_SECUENCIAL.md)
- **Workflow del Programador (incluye invocación al Tester):** [Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md](../AgenteProgramador/AGENTE_WORKFLOW.md)
- **CI (mismos comandos):** [.github/workflows/test.yml](../../.github/workflows/test.yml)
