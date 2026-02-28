# Prompt: Auto-Generation desde el código

> Usá este prompt cuando el proyecto tiene código pero le falta documentación. El agente infiere lo que puede.

---

## Instrucciones para el agente

Explorá el repositorio con profundidad. Tu objetivo es generar documentación útil basándote en lo que existe en el código, con mínima intervención del usuario.

### 1. Exploración profunda

Revisá (en este orden):
- Manifiestos: `package.json`, `Cargo.toml`, `pyproject.toml`, `go.mod`
- Estructura de carpetas (top-level y segundo nivel)
- Archivos de configuración: `.env.example`, `docker-compose.yml`, CI configs
- Código fuente: módulos principales, exports públicos, funciones clave
- Tests existentes: revelan comportamiento esperado del sistema
- Commits recientes de git si tenés acceso: revelan qué se está trabajando

### 2. Inferencia de información

Para cada documento a generar, inferí:

**Para README:**
- Nombre: del manifiesto o carpeta raíz
- Descripción: del campo `description` del manifiesto o de comentarios en archivos principales
- Instalación: de scripts en el manifiesto o de archivos como `Makefile`, `justfile`
- Uso básico: de archivos de ejemplo, tests, o README parcial existente

**Para ROADMAP:**
- Versión actual: del manifiesto
- Features implementadas: de la estructura de código y módulos existentes
- Features en progreso: de branches, TODOs en código, issues si hay acceso
- Próximos pasos: inferidos de la lógica del sistema y gaps obvios

**Para BLUEPRINT:**
- Stack: del manifiesto y código
- Arquitectura: de la estructura de carpetas y módulos
- Decisiones técnicas: de configuraciones y comentarios en código

### 3. Formato de output

Para cada archivo generado:
- Usá `<!-- GENERADO AUTOMÁTICAMENTE — revisar -->` al inicio
- Marcá con `<!-- TODO: [qué falta] -->` cada campo que no pudiste inferir
- Incluí una sección al final: `## ✅ Revisión pendiente` listando los TODOs

### 4. Presentación al usuario

Antes de guardar cualquier archivo, mostráselo al usuario y preguntá:

```
Generé el [DOCUMENTO]. ¿Lo guardamos, querés hacer cambios primero, o descartarlo?
```
