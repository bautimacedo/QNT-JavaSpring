# 🤖 Agent Bootstrap Kit

Este directorio contiene todo lo necesario para que un agente de IA pueda **ayudarte a documentar y estructurar este proyecto desde cero**.

---

## ¿Qué hay aquí?

| Carpeta / Archivo | Propósito |
|---|---|
| `prompts/` | Prompts listos para pasarle al agente según el estado del proyecto |
| `templates/` | Plantillas vacías con estructura predefinida para cada documento |
| `examples/` | Versiones mockeadas de los documentos como referencia |
| `scripts/` | Scripts de utilidad para inicializar la documentación |
| `AGENT_PROMPT.md` | **El prompt principal** — empieza aquí |

---

## 🚀 Cómo usar este kit

### Opción A — El agente toma el control (modo automático)

Pasale al agente este prompt:

```
Lee el archivo agent-bootstrap/AGENT_PROMPT.md y sigue las instrucciones.
```

El agente va a:
1. Analizar el estado actual del repositorio
2. Detectar qué documentación falta
3. Proponerte un plan de documentación
4. Generar los archivos que faltan usando las plantillas

### Opción B — Modo guiado (recomendado para proyectos sin nada)

Pasale al agente este prompt:

```
Lee agent-bootstrap/prompts/guided-discovery.md y arranca el proceso de descubrimiento del proyecto.
```

El agente va a hacerte preguntas una por una para entender el proyecto y construir la documentación contigo.

### Opción C — Generar un documento específico

```
Lee agent-bootstrap/templates/ROADMAP.template.md y generá el ROADMAP.md del proyecto raíz basándote en lo que encontrás en el código.
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
