#!/usr/bin/env bash
# Smoke test de la API: login + una petición autenticada.
# Uso: ./scripts/smoke-test-api.sh [BASE_URL]
# Requiere: la aplicación corriendo (ej. en localhost:8080) y un usuario en BD.
# Variables opcionales: SMOKE_USER, SMOKE_PASS (default: admin@ejemplo.com / admin).

set -e
BASE_URL="${1:-http://localhost:8080}"
USER="${SMOKE_USER:-admin@ejemplo.com}"
PASS="${SMOKE_PASS:-admin}"
API="${BASE_URL}/api/qnt/v1"

echo "Smoke test API: $API (user=$USER)"

# Login
RESP=$(curl -s -w "\n%{http_code}" -X POST "${API}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER}\",\"password\":\"${PASS}\"}")
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)

if [ "$CODE" != "200" ]; then
  echo "FAIL: login returned HTTP $CODE. Body: $BODY"
  exit 1
fi

TOKEN="$BODY"
if [ -z "$TOKEN" ]; then
  echo "FAIL: empty token"
  exit 1
fi

# Petición autenticada (ej. /auth/me)
ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${API}/auth/me" \
  -H "Authorization: Bearer ${TOKEN}")

if [ "$ME_CODE" != "200" ]; then
  echo "FAIL: GET /auth/me returned HTTP $ME_CODE"
  exit 1
fi

echo "OK: login 200, GET /auth/me 200"
