#!/bin/bash

# =============================================================
# init-docs.sh — Inicializar documentación en un nuevo proyecto
# =============================================================
# Uso: bash agent-bootstrap/scripts/init-docs.sh
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BOOTSTRAP_DIR="$(dirname "$SCRIPT_DIR")"
ROOT_DIR="$(dirname "$BOOTSTRAP_DIR")"
TEMPLATES_DIR="$BOOTSTRAP_DIR/templates"

echo ""
echo "🚀 Agent Bootstrap — Inicialización de documentación"
echo "======================================================"
echo "Raíz del proyecto: $ROOT_DIR"
echo ""

# Detectar archivos existentes
MISSING_DOCS=()
EXISTING_DOCS=()

check_doc() {
    local file="$ROOT_DIR/$1"
    if [ -f "$file" ]; then
        EXISTING_DOCS+=("$1")
    else
        MISSING_DOCS+=("$1")
    fi
}

check_doc "README.md"
check_doc "ROADMAP.md"
check_doc "BLUEPRINT.md"
check_doc "CHANGELOG.md"

# Mostrar diagnóstico
if [ ${#EXISTING_DOCS[@]} -gt 0 ]; then
    echo "✅ Documentos existentes:"
    for doc in "${EXISTING_DOCS[@]}"; do
        echo "   • $doc"
    done
    echo ""
fi

if [ ${#MISSING_DOCS[@]} -eq 0 ]; then
    echo "✨ El proyecto ya tiene toda la documentación base. Nada que hacer."
    exit 0
fi

echo "📋 Documentos faltantes:"
for doc in "${MISSING_DOCS[@]}"; do
    echo "   • $doc"
done
echo ""

# Preguntar qué hacer
echo "¿Qué querés hacer?"
echo "  1) Copiar TODAS las plantillas a la raíz del proyecto"
echo "  2) Elegir cuáles copiar"
echo "  3) Solo ver el diagnóstico (no copiar nada)"
echo ""
read -p "Opción [1/2/3]: " choice

copy_template() {
    local name="$1"
    local template="$TEMPLATES_DIR/${name%.md}.template.md"
    local dest="$ROOT_DIR/$name"
    
    if [ -f "$template" ]; then
        cp "$template" "$dest"
        echo "   ✓ $name creado"
    else
        echo "   ✗ No se encontró plantilla para $name"
    fi
}

case "$choice" in
    1)
        echo ""
        echo "Copiando plantillas..."
        for doc in "${MISSING_DOCS[@]}"; do
            copy_template "$doc"
        done
        echo ""
        echo "✅ Listo. Abrí los archivos y buscá '<!-- TODO:' para completar los campos."
        echo ""
        echo "💡 Tip: Pasale al agente este prompt para que te ayude a completarlos:"
        echo "   'Lee agent-bootstrap/AGENT_PROMPT.md y completá los documentos recién creados.'"
        ;;
    2)
        echo ""
        for doc in "${MISSING_DOCS[@]}"; do
            read -p "¿Copiar $doc? [s/N]: " yn
            case "$yn" in
                [sS]*)
                    copy_template "$doc"
                    ;;
                *)
                    echo "   - $doc omitido"
                    ;;
            esac
        done
        echo ""
        echo "✅ Hecho."
        ;;
    3)
        echo ""
        echo "OK, no se copió nada. Cuando quieras arrancar, correlo de nuevo o pasale a tu agente:"
        echo "   'Lee agent-bootstrap/AGENT_PROMPT.md'"
        ;;
    *)
        echo "Opción inválida. Saliendo."
        exit 1
        ;;
esac
