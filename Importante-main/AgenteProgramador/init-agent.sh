#!/bin/bash

# =============================================================
# init-agent.sh — Inicializar el sistema de agente autónomo
# =============================================================
# Uso: bash agent-bootstrap/scripts/init-agent.sh
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BOOTSTRAP_DIR="$(dirname "$SCRIPT_DIR")"
ROOT_DIR="$(dirname "$BOOTSTRAP_DIR")"

echo ""
echo "🤖 Agent Bootstrap — Sistema de agente autónomo"
echo "================================================="
echo "Raíz del proyecto: $ROOT_DIR"
echo ""

# 1. Crear carpeta .agents para logs
if [ ! -d "$ROOT_DIR/.agents" ]; then
    mkdir -p "$ROOT_DIR/.agents"
    echo "✓ Carpeta .agents/ creada (logs del agente)"
else
    echo "• .agents/ ya existe"
fi

# 2. Agregar .agents al .gitignore
GITIGNORE="$ROOT_DIR/.gitignore"
if [ -f "$GITIGNORE" ]; then
    if ! grep -q "\.agents/\*\.log" "$GITIGNORE" 2>/dev/null; then
        echo "" >> "$GITIGNORE"
        echo "# Agent logs" >> "$GITIGNORE"
        echo ".agents/*.log" >> "$GITIGNORE"
        echo "✓ .agents/*.log agregado a .gitignore"
    else
        echo "• .agents/*.log ya en .gitignore"
    fi
else
    echo "# Agent logs" > "$GITIGNORE"
    echo ".agents/*.log" >> "$GITIGNORE"
    echo "✓ .gitignore creado con .agents/*.log"
fi

# 3. Verificar que las carpetas de la cola existan
PROMPTS_DIR="$BOOTSTRAP_DIR/prompts"
for dir in pendientes en_proceso completados; do
    if [ ! -d "$PROMPTS_DIR/$dir" ]; then
        mkdir -p "$PROMPTS_DIR/$dir"
        echo "✓ prompts/$dir/ creada"
    else
        echo "• prompts/$dir/ ya existe"
    fi
done

# 4. Verificar que git está inicializado
if [ ! -d "$ROOT_DIR/.git" ]; then
    echo ""
    echo "⚠️  No se detectó repositorio git. El sistema de agente requiere git."
    read -p "¿Inicializar git ahora? [s/N]: " yn
    case "$yn" in
        [sS]*)
            cd "$ROOT_DIR"
            git init
            git add .
            git commit --no-verify -m "chore: setup inicial"
            echo "✓ Git inicializado con commit inicial"
            ;;
        *)
            echo "   Iniciá git manualmente antes de usar el agente."
            ;;
    esac
else
    echo "• Git detectado ✓"
fi

# 5. Verificar si hay prompts en pendientes
PENDING_COUNT=$(ls "$PROMPTS_DIR/pendientes/" 2>/dev/null | grep "^v" | wc -l | tr -d ' ')
if [ "$PENDING_COUNT" -eq 0 ]; then
    echo ""
    echo "📋 No hay prompts en pendientes/."
    echo "   Creá tu primer prompt usando la plantilla:"
    echo "   → agent-bootstrap/templates/PROMPT_TAREA.template.md"
    echo "   → Guardalo en: agent-bootstrap/prompts/pendientes/v0.1.0-nombre.md"
fi

echo ""
echo "══════════════════════════════════════════════════════"
echo "✅ Sistema de agente listo."
echo ""
echo "Para arrancar el agente, pasale este prompt:"
echo ""
echo "  'Lee agent-bootstrap/AGENTE_WORKFLOW.md y ejecutá el ciclo.'"
echo ""
echo "O para proyectos sin documentación previa:"
echo ""
echo "  'Lee agent-bootstrap/AGENT_PROMPT.md y seguí las instrucciones.'"
echo "══════════════════════════════════════════════════════"
