# Agentes del proyecto

Este directorio define los **agentes** que usamos para planificar, desarrollar y verificar el proyecto. Todos se pueden ejecutar **desde Cursor** (o desde otro IDE que permita invocar un agente con un prompt).

| Agente | Archivo | Rol |
|--------|---------|-----|
| **PM** | `Importante-main/AgentePM/AGENTE_PM.md` | Planificación: conversa, genera prompts en `pendientes/`, actualiza ROADMAP. Al terminar invoca al Programador (pipeline secuencial). |
| **Programador** | `Importante-main/AgenteProgramador/AGENTE_WORKFLOW.md` | Ejecución: toma un prompt de la cola, implementa, verifica (build/tests), commit/tag/merge, invoca al Tester y luego checkpoint. |
| **Tester** | `Importante-main/AgenteQA/AGENTE_TESTER.md` | Verificación: ejecuta `mvn test` (y opcionalmente smoke API) y reporta Listo para merge / No listo. No implementa código. |

## Pipeline secuencial (recomendado)

Flujo completo en una sola conversación con el PM:

1. **Vos** → "Lee Importante-main/AgentePM/AGENTE_PM.md y arrancá. Quiero [tarea]."
2. **PM** → genera el prompt y **invoca al Programador** con README + AGENTE_WORKFLOW + PROMPT_TAREA.template + @prompt generado.
3. **Programador** → implementa, commitea, mergea y **invoca al Tester**.
4. **Tester** → ejecuta tests y reporta. Si OK, el Programador da el checkpoint a vos.

Documentación detallada: **Importante-main/PIPELINE_SECUENCIAL.md**.

## Cómo invocar al Agente Tester desde Cursor

- *"Lee Importante-main/AgenteQA/AGENTE_TESTER.md y ejecutá la verificación"*
- *"Corré el agente tester"*
- *"Ejecutá los tests y reportá"*

El Tester corre los mismos comandos que el pipeline (`.github/workflows/test.yml`), así que si pasa en local, debería pasar en CI.

## Pipeline CI

En cada **push** y **pull request** a `main`/`master`, GitHub Actions ejecuta:

- `cd gestion && ./mvnw clean test -q`

No se ejecuta el smoke test de API en CI (requiere app levantada y BD con usuario); ese paso es opcional en local cuando la app está corriendo.
