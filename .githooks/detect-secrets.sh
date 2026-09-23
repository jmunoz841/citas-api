#!/usr/bin/env bash
# Detector de secretos sobre los archivos en staging.
#
# Dos niveles:
#   1. Bloqueos absolutos: archivos .env, claves privadas, credenciales de nube
#      y cadenas de conexion con contrasena. No se pueden eximir.
#   2. Patrones con lista blanca (.githooks/secrets-allowlist.txt): asignaciones
#      de credenciales y JWT, que en pruebas y documentacion son ficticios.
#
# Salida 0 = limpio, 1 = hallazgos.

set -uo pipefail

repo_root=$(git rev-parse --show-toplevel)
allowlist="$repo_root/.githooks/secrets-allowlist.txt"
findings=0

# Nivel 1: se aplican a cualquier archivo, sin excepcion.
hard_patterns='BEGIN (RSA |OPENSSH |EC |DSA |ENCRYPTED )?PRIVATE KEY|AKIA[0-9A-Z]{16}|jdbc:[a-z]+://[^[:space:]"]*:[^[:space:]"/@]+@'

# Nivel 2: exentos si la ruta coincide con la lista blanca.
soft_patterns='(password|passwd|contrasena|secret|token|api[_-]?key|apikey)[[:space:]]*[:=][[:space:]]*["'"'"'][^"'"'"']{8,}["'"'"']|eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}'

report() {
  if [ "$findings" -eq 0 ]; then
    echo "SECRETOS: posibles credenciales en los archivos preparados para commit." >&2
    echo >&2
  fi
  findings=$((findings + 1))
  echo "  $1" >&2
}

is_allowed() {
  [ -f "$allowlist" ] || return 1
  while IFS= read -r rule || [ -n "$rule" ]; do
    case "$rule" in ''|\#*) continue ;; esac
    if printf '%s' "$1" | grep -qE "$rule"; then
      return 0
    fi
  done < "$allowlist"
  return 1
}

staged=$(git diff --cached --name-only --diff-filter=ACMR)
[ -z "$staged" ] && exit 0

while IFS= read -r file || [ -n "$file" ]; do
  [ -z "$file" ] && continue

  base=$(basename "$file")
  case "$base" in
    .env|.env.*|*.pem|*.p12|*.pfx|*.jks|id_rsa|id_ed25519)
      report "$file: archivo de entorno o de claves; no debe versionarse."
      continue
      ;;
  esac

  content=$(git show ":$file" 2>/dev/null) || continue
  # Ignora binarios.
  printf '%s' "$content" | grep -qI . 2>/dev/null || continue

  hits=$(printf '%s\n' "$content" | grep -nEi "$hard_patterns" | head -3)
  if [ -n "$hits" ]; then
    while IFS= read -r hit; do
      report "$file:${hit%%:*} clave privada o credencial de conexion."
    done <<EOF
$hits
EOF
  fi

  if is_allowed "$file"; then
    continue
  fi

  hits=$(printf '%s\n' "$content" | grep -nEi "$soft_patterns" | head -3)
  if [ -n "$hits" ]; then
    while IFS= read -r hit; do
      report "$file:${hit%%:*} credencial o token en texto plano."
    done <<EOF
$hits
EOF
  fi
done <<EOF
$staged
EOF

if [ "$findings" -gt 0 ]; then
  echo >&2
  echo "Commit bloqueado. Mueve el valor a una variable de entorno del .env local," >&2
  echo "o, si es un dato ficticio legitimo, agrega la ruta a .githooks/secrets-allowlist.txt" >&2
  echo "explicando por que es seguro." >&2
  exit 1
fi

exit 0
