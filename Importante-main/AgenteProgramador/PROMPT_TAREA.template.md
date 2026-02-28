# vX.Y.Z — [Nombre descriptivo de la tarea]

**VERSIÓN:** vX.Y.Z  
**SLUG:** [slug-para-la-rama]  
**DEPENDENCIAS:** [vA.B.C, vD.E.F] | ninguna  
**ESTIMACIÓN:** [30min / 1h / 2h / medio día]  
**PRIORIDAD:** [alta / media / baja]

---

## Descripción

[1-3 párrafos describiendo qué hay que hacer y por qué.
Incluir contexto de negocio / técnico necesario para entenderlo.]

---

## PASO 0 — Contexto previo a leer

> ⚠️ Si venís del AGENTE_WORKFLOW.md, saltear este paso — ya lo hiciste.

Leer sin ejecutar nada:
- `[archivo clave 1]`
- `[archivo clave 2]`
- `[archivo clave 3]`

---

## PASO 1 — [Primera acción concreta]

[Descripción detallada de qué hacer]

```bash
# Comandos concretos si aplica
```

```
// Código a escribir/modificar si aplica
```

**Verificación:** [cómo saber que este paso está bien hecho]

---

## PASO 2 — [Segunda acción]

[Descripción]

```bash
# Comandos
```

**Verificación:** [criterio]

---

## PASO 3 — [Continuar según necesidad...]

---

## PASO N — Commit y tag

```bash
VERSION="vX.Y.Z"
SLUG="[slug]"

git add [archivos modificados]
git commit --no-verify -m "feat([scope]): [descripción concisa]

- [detalle 1]
- [detalle 2]
- [detalle 3]"

git tag -a "${VERSION}" -m "Release ${VERSION}: [nombre de la tarea]"
```

---

## Verificación final

Lista de criterios que deben cumplirse antes de considerar este prompt completo:

- [ ] [Criterio técnico 1: ej. "build sin errores"]
- [ ] [Criterio técnico 2: ej. "tests pasan"]
- [ ] [Criterio funcional: ej. "feature X visible en el UI"]
- [ ] [Criterio de calidad: ej. "sin warnings de lint"]

---

## Notas / Consideraciones

<!-- Cualquier advertencia, edge case, o contexto adicional que el agente deba saber -->

- [Nota 1]
- [Nota 2]
