# Procedimientos

Este documento define el flujo de trabajo estándar para el equipo de desarrollo,  control de versiones, colaboración, revisión y aprobación de código, y despliegues a entornos de desarrollo y producción.

## 1. Estructura de Ramas

Se adopta un flujo basado en ramas permanentes y ramas temporales.

### Ramas permanentes

- `main` (o `master`)

  - Representa el estado del código en producción
  - Siempre debe ser estable y desplegable

- `develop`

  - Representa el estado actual del desarrollo
  - Integra todas las funcionalidades aprobadas antes de pasar a producción

### Ramas temporales

- `feature/nombre-feature`

  - Desarrollo de nuevas funcionalidades
  - Se crean desde `develop`

- `fix/nombre-fix` o `bugfix/nombre-fix`

  - Corrección de errores detectados en desarrollo
  - Se crean desde `develop`

- `hotfix/nombre-hotfix`

  - Correcciones urgentes en producción
  - Se crean desde `main`

### Creación de una nueva feature

1. Actualizar la rama `develop` local

   ```bash
   git checkout develop
   git pull origin develop
   ```

2. Crear una nueva rama feature

   ```bash
   git checkout -b feature/nombre-feature
   ```

3. Desarrollar la funcionalidad

4. Commits frecuentes y descriptivos

   ```bash
   git commit -m "Agrega validación de formulario de login"
   ```

5. Subir la rama al repositorio remoto

   ```bash
   git push origin feature/nombre-feature
   ```



## 2. Pull Requests (PR)

### Creación de un Pull Request

- El PR debe apuntar siempre a `develop` (excepto hotfix)
- El título debe ser claro y descriptivo
- La descripción debe incluir:
  - Qué se hizo

  - Por qué se hizo

  - Cómo probar el cambio Todo PR debe ser revisado por al menos un desarrollador

    El autor del PR no puede aprobar su propio PR

    Testear por errores antes de aceptar el PR

###

## 3. Merge de Pull Requests

### Merge a develop

Una vez aprobado el Pull Request hacia `develop`, el merge se realiza desde GitHub. En caso de ser necesario hacerlo manualmente:

```bash
git checkout develop
git pull origin develop
git merge feature/nombre-feature
git push origin develop
```

- Verificar que no existan conflictos antes del push
- Eliminar la rama feature luego del merge

## 4. Documentación del Proyecto

### Carpeta /docs

Cada proyecto debe contener  una carpeta `/docs` en la raíz del repositorio.&#x20;

Contenido:

- **Documentación de features**

  - Por cada nueva feature desarrollada, el autor debe actualizar el archivo de features.md con las adiciones nuevas.
    - Objetivo de la feature
    - Descripción funcional
    - Cambios relevantes en el código
    - Consideraciones técnicas



- **Diagrama de base de datos**

  - Debe existir un archivo que represente el diagrama de la base de datos, incluyendo:
    - Tablas
    - Columnas
    - Relaciones
  - El formato puede ser imagen, markdown con diagramas (ej. Mermaid) o herramienta equivalente.
  - Atualizar cuando se realizan migraciones nuevas.

- **Dependencias y requerimientos**

  - Archivo .txt con:
    - Dependencias principales del proyecto
    - Versiones mínimas/recomendadas
    - Requerimientos de infraestructura relevantes\
      Mantener actualizado cuando se hace nuevo uso de librerias 

- Release docs

  - Cuando se pasan cambios nuevos significantes a produccion, se debe crear un nuevo release doc para documentar las versiones que salen a produccion, estos archivos tambien deben ser tipo .md y encontrarse en /docs/releases

## 5. Flujo de Releases

###  Preparación de release

1. Verificar que la version sea estable
2. Testear, verificar que no hayan bugs

### Merge a producción

1. Crear PR desde `develop` hacia `main`
2. Revisar y aprobar PR
3. Merge a `main`

```bash
git checkout main
git pull origin main
```

## 6. Hotfixes en Producción

### Creación de hotfix

1. Crear rama desde `main`

   ```bash
   git checkout main
   git pull origin main
   git checkout -b hotfix/nombre-hotfix
   ```

2. Aplicar corrección

3. Commit y push

### Integración del hotfix

- Crear PR hacia `main`
- Una vez mergeado:
  - Mergear también el hotfix en `develop` para mantener consistencia

## 7. Reglas Generales

- No hacer commits directamente en `main` ni `develop`
- Mantener ramas actualizadas con la base correspondiente
- Resolver conflictos localmente antes de solicitar revisión
- El código debe compilar y pasar tests antes de crear un PR

##

##

---

Este protocolo es de referencia para todo el equipo y puede ser actualizado según la evolución del proyecto o del flujo de trabajo.

