# 🤖 Agent Bootstrap Kit

Este directorio contiene todo lo necesario para que un agente de IA pueda **ayudarte a documentar y estructurar este proyecto desde cero**.

---

## ¿Qué hay aquí?

| Carpeta / Archivo | Propósito |
|---|---|
| `AGENTE_WORKFLOW.md` | **Sistema de agente autónomo con cola de tareas** — empieza aquí si el proyecto tiene tareas definidas |
| `AGENT_PROMPT.md` | Diagnóstico y generación de documentación — para proyectos sin docs |
| `prompts/` | Cola de tareas del agente (`pendientes/`, `en_proceso/`, `completados/`) + prompts de descubrimiento |
| `templates/` | Plantillas de documentos y de prompts de tarea |
| `examples/` | Versiones mockeadas de los documentos como referencia |
| `scripts/` | Scripts para inicializar docs (`init-docs.sh`) y el sistema de agente (`init-agent.sh`) |

---

## 🚀 Cómo usar este kit

### Opción A — Agente autónomo con cola de tareas (modo full)

Para proyectos con tareas definidas en `prompts/pendientes/`:

```
Lee agent-bootstrap/AGENTE_WORKFLOW.md y ejecutá el ciclo.
```

El agente va a:
1. Detectar el siguiente prompt disponible en la cola
2. Reclamarlo con lock atómico por git (soporta multi-agente)
3. Ejecutarlo con verificación técnica (build/tests/lint)
4. Commitear, taggear y mergear
5. Presentarte un checkpoint antes de continuar

### Opción B — Diagnóstico y generación de documentación

Para proyectos sin documentación, pasale al agente:

```
Lee agent-bootstrap/AGENT_PROMPT.md y seguí las instrucciones.
```

El agente analiza el repo, detecta qué falta, y genera o guía la creación.

### Opción C — Modo guiado por preguntas

Ideal para proyectos nuevos sin nada:

```
Lee agent-bootstrap/prompts/guided-discovery.md y arrancá el proceso de descubrimiento.
```

El agente te hace preguntas una por una y construye la documentación con tus respuestas.

### Opción D — Generar un documento específico

```
Lee agent-bootstrap/templates/ROADMAP.template.md y generá el ROADMAP.md del proyecto raíz.
```

---

## 📋 Documentos que este kit puede generar

- `README.md` — Descripción general del proyecto
- `ROADMAP.md` — Versiones, fases y objetivos
- `BLUEPRINT.md` — Arquitectura técnica y decisiones de diseño
- `CHANGELOG.md` — Historial de cambios por versión
- `CONTRIBUTING.md` — Guía para contribuidores
- `docs/PROJECT_OVERVIEW.md` — Visión general extendida

---

## 💡 Filosofía

> Este kit no reemplaza al desarrollador — lo amplifica.
> El agente propone, el desarrollador decide.
> Toda la documentación generada debe ser revisada y validada por el equipo.

---

## 🔄 Actualizar este kit

Si mejorás alguna plantilla o prompt, considerá contribuirlo al proyecto origen donde nació este kit.
