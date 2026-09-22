#!/usr/bin/env bash
set -uo pipefail

API="${API_URL:-http://localhost:8080}/api"
PASSWORD="${SEED_PASSWORD:-Senha@123}"
JSON='Content-Type: application/json'
passed=0
failed=0

login() {
  curl -s -X POST "$API/auth/login" -H "$JSON" -d "{\"email\":\"$1\",\"password\":\"$PASSWORD\"}" \
    | sed -nE 's/.*"accessToken":"([^"]+)".*/\1/p'
}

check() {
  local expected=$1 description=$2
  shift 2
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' "$@")
  if [[ "$status" == "$expected" ]]; then
    printf '  ok    %s  %s\n' "$status" "$description"
    passed=$((passed + 1))
  else
    printf '  FALHA %s  %s (esperado %s)\n' "$status" "$description" "$expected"
    failed=$((failed + 1))
  fi
}

ADMIN=$(login admin@sistema.local)
OPERATOR=$(login operador@sistema.local)
CLIENT=$(login cliente@sistema.local)

if [[ -z "$ADMIN" || -z "$OPERATOR" || -z "$CLIENT" ]]; then
  echo "Login das contas de demonstração falhou. A API está no ar em $API e com SEED_ENABLED=true?"
  exit 1
fi

CLIENT_ID=$(curl -s "$API/usuarios/me" -H "Authorization: Bearer $CLIENT" | sed -nE 's/^\{"id":([0-9]+).*/\1/p')
ADMIN_ID=$(curl -s "$API/usuarios/me" -H "Authorization: Bearer $ADMIN" | sed -nE 's/^\{"id":([0-9]+).*/\1/p')
PROBE_EMAIL="teste.$(date +%s)@sistema.local"

echo "Autenticação"
check 401 "login com senha errada" -X POST "$API/auth/login" -H "$JSON" -d '{"email":"admin@sistema.local","password":"errada"}'
check 401 "login com e-mail inexistente" -X POST "$API/auth/login" -H "$JSON" -d '{"email":"ninguem@sistema.local","password":"errada"}'
check 401 "requisição sem token" "$API/usuarios"
check 401 "token com assinatura adulterada" "$API/usuarios" -H "Authorization: Bearer ${ADMIN}x"

echo "Consulta"
check 200 "ADMIN lista usuários" "$API/usuarios" -H "Authorization: Bearer $ADMIN"
check 200 "OPERATOR lista usuários" "$API/usuarios" -H "Authorization: Bearer $OPERATOR"
check 403 "CLIENT lista usuários" "$API/usuarios" -H "Authorization: Bearer $CLIENT"
check 200 "CLIENT consulta /me" "$API/usuarios/me" -H "Authorization: Bearer $CLIENT"
check 200 "CLIENT consulta o próprio ID" "$API/usuarios/$CLIENT_ID" -H "Authorization: Bearer $CLIENT"
check 403 "CLIENT consulta outro usuário" "$API/usuarios/$ADMIN_ID" -H "Authorization: Bearer $CLIENT"
check 404 "ADMIN consulta ID inexistente" "$API/usuarios/999999" -H "Authorization: Bearer $ADMIN"

echo "Cadastro"
check 403 "OPERATOR cria usuário" -X POST "$API/usuarios" -H "Authorization: Bearer $OPERATOR" -H "$JSON" \
  -d "{\"name\":\"Teste\",\"email\":\"$PROBE_EMAIL\",\"password\":\"Senha@123\",\"role\":\"CLIENT\"}"
check 400 "ADMIN cria com dados inválidos" -X POST "$API/usuarios" -H "Authorization: Bearer $ADMIN" -H "$JSON" \
  -d '{"name":"","email":"invalido","password":"123","role":"CLIENT"}'
CREATED=$(curl -s -X POST "$API/usuarios" -H "Authorization: Bearer $ADMIN" -H "$JSON" \
  -d "{\"name\":\"Teste\",\"email\":\"$PROBE_EMAIL\",\"password\":\"Senha@123\",\"role\":\"CLIENT\"}")
NEW_ID=$(sed -nE 's/^\{"id":([0-9]+).*/\1/p' <<< "$CREATED")
if [[ -n "$NEW_ID" ]]; then
  printf '  ok    201  ADMIN cria usuário (id %s)\n' "$NEW_ID"
  passed=$((passed + 1))
else
  printf '  FALHA      ADMIN cria usuário: %s\n' "$CREATED"
  failed=$((failed + 1))
fi
check 409 "ADMIN cria com e-mail repetido" -X POST "$API/usuarios" -H "Authorization: Bearer $ADMIN" -H "$JSON" \
  -d "{\"name\":\"Teste\",\"email\":\"$PROBE_EMAIL\",\"password\":\"Senha@123\",\"role\":\"CLIENT\"}"

echo "Atualização"
check 200 "OPERATOR edita dados de um cliente" -X PUT "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $OPERATOR" -H "$JSON" \
  -d "{\"name\":\"Teste Editado\",\"email\":\"$PROBE_EMAIL\",\"role\":\"CLIENT\"}"
check 403 "OPERATOR altera perfil de um cliente" -X PUT "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $OPERATOR" -H "$JSON" \
  -d "{\"name\":\"Teste\",\"email\":\"$PROBE_EMAIL\",\"role\":\"ADMIN\"}"
check 403 "OPERATOR edita um administrador" -X PUT "$API/usuarios/$ADMIN_ID" -H "Authorization: Bearer $OPERATOR" -H "$JSON" \
  -d '{"name":"X","email":"admin@sistema.local","role":"ADMIN"}'
check 403 "CLIENT edita o próprio cadastro" -X PUT "$API/usuarios/$CLIENT_ID" -H "Authorization: Bearer $CLIENT" -H "$JSON" \
  -d '{"name":"X","email":"cliente@sistema.local","role":"CLIENT"}'
check 200 "ADMIN promove para OPERATOR" -X PUT "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $ADMIN" -H "$JSON" \
  -d "{\"name\":\"Teste\",\"email\":\"$PROBE_EMAIL\",\"role\":\"OPERATOR\"}"

echo "Exclusão"
check 403 "OPERATOR exclui usuário" -X DELETE "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $OPERATOR"
check 409 "ADMIN exclui a própria conta" -X DELETE "$API/usuarios/$ADMIN_ID" -H "Authorization: Bearer $ADMIN"
check 204 "ADMIN exclui usuário" -X DELETE "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $ADMIN"
check 404 "usuário excluído não existe mais" "$API/usuarios/$NEW_ID" -H "Authorization: Bearer $ADMIN"

echo
echo "$passed passaram, $failed falharam"
[[ $failed -eq 0 ]]
