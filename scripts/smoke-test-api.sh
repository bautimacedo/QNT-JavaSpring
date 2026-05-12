#!/usr/bin/env bash
# Smoke test de la API: login + peticiones autenticadas + verificación de seguridad.
# Uso: ./scripts/smoke-test-api.sh [BASE_URL]
# Variables opcionales: SMOKE_USER, SMOKE_PASS (default: admin@ejemplo.com / admin)
# Requiere: la aplicación corriendo (ej. en localhost:8080) y un usuario en BD.

set -e
BASE_URL="${1:-http://localhost:8080}"
USER="${SMOKE_USER:-admin@ejemplo.com}"
PASS="${SMOKE_PASS:-admin}"
API="${BASE_URL}/api/qnt/v1"
PASS_COUNT=0
FAIL_COUNT=0

ok()   { echo "  PASS: $1"; PASS_COUNT=$((PASS_COUNT+1)); }
fail() { echo "  FAIL: $1"; FAIL_COUNT=$((FAIL_COUNT+1)); }

echo "========================================"
echo "Smoke test API: $API (user=$USER)"
echo "========================================"

# ── 1. Login ──────────────────────────────────────────────────────────────────
echo ""
echo "[1] Autenticación"
RESP=$(curl -s -w "\n%{http_code}" -X POST "${API}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER}\",\"password\":\"${PASS}\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)

if [ "$CODE" = "200" ] && [ -n "$BODY" ]; then
  ok "POST /auth/login → 200"
  TOKEN="$BODY"
else
  fail "POST /auth/login → HTTP $CODE (esperado 200). Body: $BODY"
  echo "Abortando: sin token no se pueden ejecutar el resto de los tests."
  exit 1
fi

ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${API}/auth/me" \
  -H "Authorization: Bearer ${TOKEN}")
[ "$ME_CODE" = "200" ] && ok "GET /auth/me con JWT → 200" || fail "GET /auth/me con JWT → $ME_CODE (esperado 200)"

UNAUTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${API}/auth/me")
[ "$UNAUTH_CODE" = "401" ] && ok "GET /auth/me sin JWT → 401" || fail "GET /auth/me sin JWT → $UNAUTH_CODE (esperado 401)"

WRONG_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${API}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"noexiste@x.com","password":"wrong"}')
[ "$WRONG_CODE" = "401" ] && ok "Login credenciales incorrectas → 401" || fail "Login credenciales incorrectas → $WRONG_CODE (esperado 401)"

# ── 2. Security headers ────────────────────────────────────────────────────────
echo ""
echo "[2] Security headers"
HEADERS=$(curl -sI "${API}/clima" 2>/dev/null)
echo "$HEADERS" | grep -qi "X-Frame-Options"         && ok "X-Frame-Options presente"         || fail "X-Frame-Options ausente"
echo "$HEADERS" | grep -qi "X-Content-Type-Options"  && ok "X-Content-Type-Options presente"  || fail "X-Content-Type-Options ausente"
echo "$HEADERS" | grep -qi "Strict-Transport-Security" && ok "HSTS presente"                  || fail "HSTS ausente"
echo "$HEADERS" | grep -qi "Content-Security-Policy" && ok "CSP presente"                     || fail "CSP ausente"

# ── 3. Rate limiting ────────────────────────────────────────────────────────────
echo ""
echo "[3] Rate limiting (envía 11 logins rápidos con usuario falso)"
for i in $(seq 1 11); do
  RATE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${API}/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ratelimit@test.com","password":"wrong"}')
  if [ "$i" -eq 11 ] && [ "$RATE_CODE" = "429" ]; then
    ok "Request #11 devolvió 429 (rate limit activo)"
  elif [ "$i" -eq 11 ]; then
    fail "Request #11 devolvió $RATE_CODE (esperado 429 — rate limit puede no estar activo)"
  fi
done

# ── 4. IDOR check ──────────────────────────────────────────────────────────────
echo ""
echo "[4] IDOR — /misiones/piloto/{id}"
IDOR_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${API}/misiones/piloto/99999" \
  -H "Authorization: Bearer ${TOKEN}")
[ "$IDOR_CODE" = "403" ] || [ "$IDOR_CODE" = "200" ] && ok "GET /misiones/piloto/99999 → $IDOR_CODE (ADMIN puede ver todo, user normal → 403)" \
  || fail "GET /misiones/piloto/99999 → $IDOR_CODE inesperado"

# ── 5. Paginación en vuelos-log ────────────────────────────────────────────────
echo ""
echo "[5] Paginación en vuelos-log"
VUELOS_RESP=$(curl -s "${API}/vuelos-log?page=0&size=5" \
  -H "Authorization: Bearer ${TOKEN}")
echo "$VUELOS_RESP" | grep -q '"data"'  && ok "GET /vuelos-log?size=5 → respuesta tiene 'data'" || fail "GET /vuelos-log?size=5 → falta campo 'data'"
echo "$VUELOS_RESP" | grep -q '"total"' && ok "GET /vuelos-log?size=5 → respuesta tiene 'total'" || fail "GET /vuelos-log?size=5 → falta campo 'total'"

# ── 6. Dashboard stats ─────────────────────────────────────────────────────────
echo ""
echo "[6] Dashboard stats (N+1 fix)"
DASH_RESP=$(curl -s "${API}/dashboard/stats" \
  -H "Authorization: Bearer ${TOKEN}")
echo "$DASH_RESP" | grep -q '"dronesTotal"'     && ok "GET /dashboard/stats → tiene 'dronesTotal'"     || fail "GET /dashboard/stats → falta 'dronesTotal'"
echo "$DASH_RESP" | grep -q '"dronesOperativos"' && ok "GET /dashboard/stats → tiene 'dronesOperativos'" || fail "GET /dashboard/stats → falta 'dronesOperativos'"

# ── 7. Endpoint interno sin secret ────────────────────────────────────────────
echo ""
echo "[7] Endpoint interno sin secret"
INT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${API}/internal/misiones/completar-por-drone" \
  -H "Content-Type: application/json" \
  -d '{"dronNombre":"test"}')
[ "$INT_CODE" = "401" ] && ok "POST /internal sin X-Internal-Secret → 401" || fail "POST /internal sin X-Internal-Secret → $INT_CODE (esperado 401)"

# ── Resumen ────────────────────────────────────────────────────────────────────
echo ""
echo "========================================"
echo "Resultado: $PASS_COUNT PASS  /  $FAIL_COUNT FAIL"
echo "========================================"
[ "$FAIL_COUNT" -eq 0 ] && exit 0 || exit 1
