#!/usr/bin/env bash
# Sobe o SUT do zero com o override de teste (tests/infra/docker-compose.qa.yml).
#
# A build das imagens do SUT baixa dependências de repositórios externos (Maven Central, npm)
# sujeitos a falhas transitórias (ex.: 502 do Maven Central). Por isso a build tem nova tentativa,
# com espera crescente; o código do SUT e seus Dockerfiles não são alterados.
#
# Uso: scripts/start-sut.sh            (BUILD_ATTEMPTS=3 por padrão)

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

attempts="${BUILD_ATTEMPTS:-3}"
compose=(docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml)

for ((attempt = 1; attempt <= attempts; attempt++)); do
  if "${compose[@]}" build; then
    break
  fi
  if ((attempt == attempts)); then
    echo "ERRO: build do SUT falhou após ${attempts} tentativas" >&2
    exit 1
  fi
  wait_seconds=$((attempt * 20))
  # Anotação visível no resumo do GitHub Actions (texto simples fora dele).
  echo "::warning title=Build do SUT::falha na tentativa ${attempt}/${attempts} (dependência externa?); nova tentativa em ${wait_seconds}s"
  sleep "${wait_seconds}"
done

"${compose[@]}" up -d
