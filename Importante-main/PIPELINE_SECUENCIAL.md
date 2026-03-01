# Pipeline secuencial — PM → Programador → Tester

Este documento define el **pipeline de producción** del proyecto: una única conversación con el PM que orquesta automáticamente al Programador y al Tester.

---

## Flujo general

```
  Tú (usuario)
       │
       │ "Quiero [tarea/feature]"
       ▼
  ┌─────────────┐
  │ AGENTE PM   │  Planifica, define scope, genera el prompt de tarea
  └──────┬──────┘
         │ Al terminar: invoca al Programador con prompt generado
         ▼
  ┌─────────────────┐
  │ AGENTE PROGRAMADOR │  Toma el prompt, implementa, commit, tag, merge
  └──────┬──────────────┘
         │ Al terminar: invoca al Tester
         ▼
  ┌─────────────┐
  │ AGENTE TESTER │  Ejecuta build + tests + (opcional) smoke API, reporta
  └──────┬──────┘
         │ Reporte: OK / No listo
         ▼
  Checkpoint: tú decidís si seguir con otra tarea o parar
```

**Ventaja:** Solo le hablás al PM. El PM llama al Programador; el Programador llama al Tester. No tenés que invocar cada agente a mano.

---

## Cómo arrancar el pipeline

### Desde Cursor (recomendado)

1. **Abrí una conversación con el PM** y describí la tarea en lenguaje de negocio:

   ```
   Lee Importante-main/AgentePM/AGENTE_PM.md y arrancá.
   Quiero [descripción de la tarea/feature].
   ```

2. El PM va a:
   - Hacerte preguntas de clarificación si hace falta
   - Definir la versión (vX.Y.Z) y el scope
   - Generar el prompt en `agent-bootstrap/prompts/pendientes/[VERSION]-[slug].md`
   - **Invocar al Agente Programador** pasando:
     - `@Importante-main/AgenteProgramador/README.md`
     - `@Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md`
     - `@Importante-main/AgenteProgramador/PROMPT_TAREA.template.md`
     - `@agent-bootstrap/prompts/pendientes/[archivo-generado].md` (o el que esté en `en_proceso/` según el flujo)

3. El Programador va a:
   - Reclamar el prompt (lock por rama git)
   - Implementar paso a paso
   - Verificación técnica (build/tests)
   - Commit, tag, merge
   - **Invocar al Agente Tester** antes del checkpoint

4. El Tester va a:
   - Ejecutar `./gestion/mvnw -f gestion/pom.xml clean test`
   - Opcional: smoke test API si la app está levantada
   - Emitir reporte: Listo para merge / No listo

5. Si el Tester reporta **No listo**, el Programador debe corregir y volver a pasar al Tester antes de dar el checkpoint al usuario.

---

## Archivos clave por agente

| Agente      | Archivo principal | Invocación típica |
|------------|-------------------|-------------------|
| **PM**     | `Importante-main/AgentePM/AGENTE_PM.md` | "Lee AGENTE_PM.md y arrancá. Quiero [tarea]." |
| **Programador** | `Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md` | Invocado por el PM con README + WORKFLOW + template + @prompt |
| **Tester** | `Importante-main/AgenteQA/AGENTE_TESTER.md` | Invocado por el Programador al terminar; o manual: "Lee AGENTE_TESTER y ejecutá la verificación" |

---

## Reglas del pipeline

1. **Un solo hilo por tarea:** PM → Programador → Tester. No saltear al Tester sin pasar por el Programador para esa versión.
2. **El PM no ejecuta código:** solo genera prompts y actualiza ROADMAP. Al terminar, delega explícitamente al Programador.
3. **El Programador no da checkpoint sin Tester:** antes del PASO 8.5 (checkpoint), debe invocar al Tester. Si los tests fallan, corregir y repetir verificación.
4. **El Tester no modifica código** por defecto; solo ejecuta y reporta. Si el usuario pide "corregí los tests que fallen", entonces sí puede proponer cambios.
5. **CI alineado:** `.github/workflows/test.yml` ejecuta los mismos comandos que el Tester (build + tests), para que lo que pasa en local pase en CI.

---

## Modo "solo ejecución" (sin PM)

Si ya tenés prompts en `pendientes/` y querés ejecutar sin pasar por el PM:

```
Lee Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md y ejecutá el ciclo.
```

El Programador tomará el siguiente prompt disponible y, al terminar, invocará al Tester igual que en el pipeline completo.

---

## Modo "solo verificación" (solo Tester)

Para correr solo la batería de tests (por ejemplo después de un cambio manual):

```
Lee Importante-main/AgenteQA/AGENTE_TESTER.md y ejecutá la verificación.
```

O desde la raíz del repo: ejecutar `./gestion/mvnw -f gestion/pom.xml clean test` y opcionalmente `./scripts/smoke-test-api.sh` si la app está levantada.

---

## Resumen de rutas

- **Pipeline completo (empezar por el PM):**  
  `Importante-main/AgentePM/AGENTE_PM.md` + tu descripción de tarea.

- **Programador (con prompt ya generado):**  
  `Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md` + contexto (README, template, @prompt).

- **Tester (verificación):**  
  `Importante-main/AgenteQA/AGENTE_TESTER.md`.

- **CEO (orquestador de sesión):**  
  `Importante-main/AgenteCEO/AGENTE_CEO.md` — para ver estado, planificar o ejecutar desde una única interfaz.
