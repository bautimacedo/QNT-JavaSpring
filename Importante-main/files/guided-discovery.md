# Prompt: Guided Discovery (Modo Guiado)

> Usá este prompt cuando el proyecto no tiene NADA de documentación y necesitás construirla desde cero con el usuario.

---

## Instrucciones para el agente

Vas a hacer un proceso de descubrimiento conversacional. **Una pregunta a la vez.** Esperá la respuesta antes de continuar. Tomá notas internas de cada respuesta — al final vas a usarlas para generar los documentos.

Empezá con este mensaje:

---

**Mensaje inicial:**

```
Hola! Voy a ayudarte a documentar este proyecto. Vamos a ir de a poco — te hago preguntas cortas y con las respuestas generamos todo juntos.

Empecemos por lo básico:

**¿Cómo se llama el proyecto y en una línea, qué hace?**
```

---

## Secuencia de preguntas

Seguí este orden. Adaptá el lenguaje al contexto del usuario.

### Bloque 1 — Identidad del proyecto

1. Nombre y descripción en una línea
2. ¿Para quién es? (usuarios finales, devs, empresas, uso interno)
3. ¿En qué estado está? (idea, prototipo, en producción, mantenimiento)

### Bloque 2 — Stack técnico

4. ¿Qué lenguajes/frameworks principales usás?
5. ¿Tiene backend? ¿Frontend? ¿Ambos? ¿Es una lib/CLI/API?
6. ¿Dónde corre? (web, desktop, mobile, servidor, edge)

### Bloque 3 — Arquitectura

7. ¿Cuáles son los módulos o partes principales del sistema?
8. ¿Hay alguna decisión técnica importante que tomaste? (por qué X tecnología, por qué esa arquitectura)
9. ¿Qué dependencias externas clave tiene?

### Bloque 4 — Roadmap

10. ¿Qué funcionalidades ya están implementadas?
11. ¿Qué estás construyendo ahora mismo?
12. ¿Qué viene después? ¿Hay versiones o fases planificadas?

### Bloque 5 — Contribución y equipo

13. ¿Es un proyecto personal o trabajan en equipo?
14. ¿Hay algún proceso para contribuir (PRs, branches, tests obligatorios)?
15. ¿Tiene licencia? ¿Cuál?

---

## Al terminar las preguntas

Presentá un resumen de lo que recopilaste y preguntá:

```
Perfecto, tengo suficiente para arrancar. Con esto puedo generar:

- [ ] README.md
- [ ] ROADMAP.md  
- [ ] BLUEPRINT.md
- [ ] CHANGELOG.md

¿Por cuál empezamos?
```

Luego generá el documento elegido usando la plantilla correspondiente de `agent-bootstrap/templates/` y completándola con las respuestas del usuario.
