# Blueprint — [NOMBRE DEL PROYECTO]

> Documento de arquitectura técnica. Describe las decisiones de diseño, estructura del sistema y principios técnicos del proyecto.

---

## 🎯 Visión técnica

<!-- TODO: En 2-3 párrafos, describir QUÉ construimos y POR QUÉ así -->

[Descripción de la visión técnica del sistema]

---

## 🏗️ Stack tecnológico

| Capa | Tecnología | Razón de la elección |
|------|-----------|---------------------|
| [Frontend / Cliente] | [Tecnología] | <!-- TODO: Por qué esta y no otra --> |
| [Backend / Servidor] | [Tecnología] | <!-- TODO --> |
| [Base de datos] | [Tecnología] | <!-- TODO --> |
| [Build / Tooling] | [Tecnología] | <!-- TODO --> |
| [Testing] | [Tecnología] | <!-- TODO --> |
| [CI/CD] | [Tecnología] | <!-- TODO --> |

---

## 📁 Estructura del proyecto

```
[nombre-proyecto]/
│
├── [módulo-1]/                 # [Responsabilidad]
│   ├── [sub-módulo]/
│   └── ...
│
├── [módulo-2]/                 # [Responsabilidad]
│   └── ...
│
├── [módulo-config]/            # [Configuración / setup]
│
└── [módulo-tests]/             # [Tests]
```

---

## 🔄 Flujo de datos principal

<!-- TODO: Describir el flujo de datos más importante del sistema -->

```
[Entrada] → [Capa 1] → [Capa 2] → [Salida]
```

1. **[Paso 1]:** [Descripción]
2. **[Paso 2]:** [Descripción]
3. **[Paso 3]:** [Descripción]

---

## 🧩 Módulos principales

### [Módulo 1]

**Responsabilidad:** [Qué hace este módulo]  
**Interfaz pública:** [Qué expone hacia afuera]  
**Dependencias:** [De qué depende]

### [Módulo 2]

**Responsabilidad:** [Qué hace este módulo]  
**Interfaz pública:** [Qué expone hacia afuera]  
**Dependencias:** [De qué depende]

<!-- TODO: Agregar un módulo por cada componente significativo -->

---

## 🔑 Decisiones de arquitectura (ADRs)

### ADR-001: [Título de la decisión]

**Fecha:** [fecha]  
**Estado:** Aceptado / Rechazado / Deprecado

**Contexto:** [Por qué había que tomar esta decisión]

**Decisión:** [Qué se decidió]

**Consecuencias:** [Qué implica esta decisión — pros y contras]

<!-- TODO: Agregar un ADR por cada decisión técnica importante -->

---

## ⚡ Principios de diseño

<!-- TODO: Listar los principios que guían las decisiones técnicas del proyecto -->

1. **[Principio 1]:** [Descripción]
2. **[Principio 2]:** [Descripción]
3. **[Principio 3]:** [Descripción]

---

## 🚫 Limitaciones conocidas

<!-- TODO: Documentar limitaciones técnicas actuales y deuda técnica -->

- **[Limitación 1]:** [Descripción y posible solución futura]
- **[Limitación 2]:** [Descripción]

---

## 📚 Referencias

<!-- TODO: Links a documentación externa relevante, papers, inspiraciones -->

- [Recurso 1](url)
- [Recurso 2](url)
