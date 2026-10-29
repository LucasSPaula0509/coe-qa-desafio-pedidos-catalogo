#!/usr/bin/env bash
# Aguarda o ambiente do SUT ficar pronto e o aquece antes das suítes (critério de entrada do
# plano de testes §4; mitigação do risco RP-09 — picos de latência em JVMs recém-iniciadas).
#
# Uso: scripts/wait-for-sut.sh [timeout-em-segundos]   (padrão: 300)
# Endereços sobrescrevíveis: ORDER_BASE_URL, CATALOG_BASE_URL, WEB_BASE_URL, CATALOG_PROXY_ADMIN_URL.

set -euo pipefail

timeout="${1:-300}"
order="${ORDER_BASE_URL:-http://localhost:8080}"
catalog="${CATALOG_BASE_URL:-http://localhost:8081}"
web="${WEB_BASE_URL:-http://localhost:4200}"
proxy="${CATALOG_PROXY_ADMIN_URL:-http://localhost:8089}"

wait_for() {
  local name="$1" url="$2" expected="${3:-}"
  local deadline=$((SECONDS + timeout))
  until body="$(curl -fsS --max-time 5 "${url}" 2>/dev/null)" && [[ -z "${expected}" || "${body}" == *"${expected}"* ]]; do
    if (( SECONDS >= deadline )); then
      echo "ERRO: ${name} não ficou pronto em ${timeout}s (${url})" >&2
      exit 1
    fi
    sleep 2
  done
  echo "OK: ${name} pronto"
}

wait_for "order-service" "${order}/actuator/health" '"UP"'
wait_for "catalog-service" "${catalog}/actuator/health" '"UP"'
wait_for "proxy de falhas" "${proxy}/__admin/mappings" 'catalog-service'
wait_for "front-end (proxy /api/orders)" "${web}/api/orders?page=0&size=1" 'content'
wait_for "massa inicial (PROD-001)" "${catalog}/api/v1/products/PROD-001" 'Notebook'

# Aquecimento: exercita os caminhos quentes (JIT, pools de conexão) sem efeito nos testes.
for _ in $(seq 1 15); do
  curl -fsS -o /dev/null "${order}/api/v1/orders?page=0&size=20"
  curl -fsS -o /dev/null "${catalog}/api/v1/products/PROD-002"
  curl -fsS -o /dev/null -X POST "${catalog}/api/v1/products/prices" \
    -H 'Content-Type: application/json' -d '{"productIds":["PROD-001","PROD-002","PROD-003"]}'
done
for i in $(seq 1 5); do
  curl -fsS -o /dev/null -X POST "${order}/api/v1/orders" -H 'Content-Type: application/json' \
    -d "{\"client\":{\"name\":\"Aquecimento ${i}\",\"document\":\"12345678901\",\"email\":\"warmup.${i}@example.com\"},\"items\":[{\"productId\":\"PROD-002\",\"quantity\":1}]}"
done
echo "OK: ambiente aquecido"
