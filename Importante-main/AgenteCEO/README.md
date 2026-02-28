# 🤖 Agent Bootstrap Kit

Este directorio contiene todo lo necesario para que un agente de IA pueda **ayudarte a documentar y estructurar este proyecto desde cero**.

---

## ¿Qué hay aquí?

| Carpeta / Archivo | Propósito |
|---|---|
| `AGENTE_CEO.md` | **Orquestador** — interfaz principal, delega al PM y al DEV según lo que el usuario necesite |
| `AGENTE_PM.md` | **Agente de planificación** — conversa con el equipo/cliente y genera prompts en `pendientes/` |
| `AGENTE_DEV.md` | **Agente de ejecución** — toma prompts de la cola y los implementa con lock multi-agente |
| `AGENT_PROMPT.md` | Diagnóstico y generación de documentación — para proyectos sin docs |
| `prompts/` | Cola de tareas del agente (`pendientes/`, `en_proceso/`, `completados/`) + prompts de descubrimiento |
| `templates/` | Plantillas de documentos y de prompts de tarea |
| `examples/` | Versiones mockeadas de los documentos como referencia |
| `scripts/` | Scripts para inicializar docs (`init-docs.sh`) y el sistema de agente (`init-agent.sh`) |

---

## 🚀 Cómo usar este kit

### Opción A — Modo CEO: orquestador completo (recomendado)

El punto de entrada principal. Lee el estado y te pregunta cómo seguir:

```
Lee agent-bootstrap/AGENTE_CEO.md y arrancá.
```

El CEO lee el estado del proyecto, te presenta un panorama y según lo que respondas delega al AGENTE_PM (para planificar) o al AGENTE_DEV (para ejecutar). Después retoma el control y te pregunta de nuevo.

### Opción B — Agente de ejecución directo

Para ir directo a ejecutar la próxima versión de la cola:

```
Lee agent-bootstrap/AGENTE_DEV.md y ejecutá el ciclo.
```

El agente va a:
1. Detectar el siguiente prompt disponible en la cola
2. Reclamarlo con lock atómico por git (soporta multi-agente)
3. Ejecutarlo con verificación técnica (build/tests/lint)
4. Commitear, taggear y mergear
5. Presentarte un checkpoint antes de continuar

### Opción C — Diagnóstico y generación de documentación

Para proyectos sin documentación, pasale al agente:

```
Lee agent-bootstrap/AGENT_PROMPT.md y seguí las instrucciones.
```

El agente analiza el repo, detecta qué falta, y genera o guía la creación.

### Opción D — Modo guiado por preguntas

Ideal para proyectos nuevos sin nada:

```
Lee agent-bootstrap/prompts/guided-discovery.md y arrancá el proceso de descubrimiento.
```

El agente te hace preguntas una por una y construye la documentación con tus respuestas.

### Opción E — Sesión de planificación con el PM (genera prompts)

Para planificar qué viene después, incorporar pedidos del cliente, o reorganizar el roadmap:

```
Lee agent-bootstrap/AGENTE_PM.md y arrancá una sesión de planificación.
```

Modos disponibles dentro del agente PM:
- `"qué sigue"` → revisa el estado y sugiere próximas versiones
- `"quiero agregar [feature]"` → define una feature nueva y genera el prompt
- `"modo cliente"` → lenguaje no técnico, traduce requerimientos a versiones
- `"reorganizar"` → reordena prioridades del roadmap

Su output son archivos en `prompts/pendientes/` listos para el agente de ejecución.

### Opción F — Generar un documento específico

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
