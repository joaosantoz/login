# Documentação da API

Base: `http://localhost:8080/api`.
Formato: JSON. Erros no formato Problem Details (RFC 9457).
Autenticação: `Authorization: Bearer <token>` em todos os endpoints, exceto login.

## 1. Endpoints

| Método | Endpoint | Finalidade | Sucesso | Perfis |
|---|---|---|---|---|
| POST | `/api/auth/login` | Autenticar e obter JWT | 200 OK | público |
| GET | `/api/usuarios` | Listar usuários | 200 OK | ADMIN, OPERATOR |
| GET | `/api/usuarios/me` | Consultar o próprio cadastro | 200 OK | todos |
| GET | `/api/usuarios/{id}` | Consultar um usuário | 200 OK | ADMIN, OPERATOR, ou o próprio usuário |
| POST | `/api/usuarios` | Criar usuário | 201 Created + `Location` | ADMIN |
| PUT | `/api/usuarios/{id}` | Atualizar nome, e-mail e perfil | 200 OK | ADMIN, OPERATOR (com restrições) |
| DELETE | `/api/usuarios/{id}` | Excluir usuário | 204 No Content | ADMIN |

### Códigos de erro

| Código | Quando |
|---|---|
| 400 Bad Request | Payload inválido (campo ausente, e-mail malformado, senha fora de 8 a 72 caracteres, perfil inexistente, JSON malformado) |
| 401 Unauthorized | Credenciais inválidas no login; token ausente, expirado, com assinatura inválida ou emissor diferente |
| 403 Forbidden | Token válido, mas o perfil não permite a operação |
| 404 Not Found | Usuário não existe |
| 409 Conflict | E-mail já cadastrado; administrador tentando excluir a própria conta |

### Contratos

`POST /api/auth/login`

```json
{ "email": "admin@sistema.local", "password": "Senha@123" }
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-09-22T02:15:03Z",
  "user": { "id": 1, "name": "Ana Administradora", "email": "admin@sistema.local", "role": "ADMIN",
            "createdAt": "2026-09-22T01:45:03Z", "updatedAt": "2026-09-22T01:45:03Z" }
}
```

`POST /api/usuarios`

```json
{ "name": "Bruno", "email": "bruno@empresa.com", "password": "Senha@123", "role": "CLIENT" }
```

`PUT /api/usuarios/{id}` substitui o recurso editável, portanto os três campos são obrigatórios:

```json
{ "name": "Bruno Silva", "email": "bruno@empresa.com", "role": "CLIENT" }
```

Resposta de usuário (GET, POST, PUT). O hash da senha nunca é serializado:

```json
{ "id": 4, "name": "Bruno", "email": "bruno@empresa.com", "role": "CLIENT",
  "createdAt": "2026-09-22T01:45:20Z", "updatedAt": "2026-09-22T01:45:20Z" }
```

Erro de validação:

```json
{
  "status": 400, "title": "Bad Request", "detail": "Dados inválidos", "instance": "/api/usuarios",
  "errors": { "password": "tamanho deve ser entre 8 e 72", "email": "deve ser um endereço de e-mail bem formado" }
}
```

E-mails são normalizados (trim e minúsculas) antes de gravar e de autenticar, e são únicos (validação no serviço e constraint `uk_users_email` no banco).

### Exemplos

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sistema.local","password":"Senha@123"}' | jq -r .accessToken)

curl -s localhost:8080/api/usuarios -H "Authorization: Bearer $TOKEN"

curl -si -X POST localhost:8080/api/usuarios -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bruno","email":"bruno@empresa.com","password":"Senha@123","role":"CLIENT"}'

curl -s -X PUT localhost:8080/api/usuarios/4 -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bruno Silva","email":"bruno@empresa.com","role":"OPERATOR"}'

curl -si -X DELETE localhost:8080/api/usuarios/4 -H "Authorization: Bearer $TOKEN"
```

## 2. Autenticação com JWT

### Processo de login

1. O cliente envia `POST /api/auth/login` com `email` e `password` no corpo JSON, sobre HTTPS em produção. Credenciais nunca vão na URL.
2. O `AuthenticationManager` delega ao `DaoAuthenticationProvider`, que carrega o usuário pelo e-mail e compara a senha com o hash BCrypt usando `PasswordEncoder.matches`.
3. Usuário inexistente e senha errada produzem a mesma resposta (`401`, "Credenciais inválidas"). O provider também executa uma comparação BCrypt quando o usuário não existe, o que evita distinguir os dois casos pelo tempo de resposta.

### Geração do token

O `TokenService` monta o conjunto de claims e o `JwtEncoder` (Nimbus) assina com HMAC-SHA256 usando a chave `JWT_SECRET`. A aplicação recusa subir se a chave tiver menos de 32 bytes, o mínimo para HS256.

Em cada requisição, o `BearerTokenAuthenticationFilter` do Spring Security extrai o token, e o `JwtDecoder` valida assinatura, algoritmo (apenas HS256), `exp`, `nbf` e `iss`. A claim `role` é convertida na authority `ROLE_<perfil>`, usada pelas regras de autorização. Não há sessão no servidor (`STATELESS`).

### Informações no token

| Claim | Exemplo | Conteúdo |
|---|---|---|
| `sub` | `"1"` | ID do usuário |
| `name` | `"Ana Administradora"` | Nome |
| `email` | `"admin@sistema.local"` | E-mail |
| `role` | `"ADMIN"` | Perfil de acesso |
| `iss` | `"sistema-api"` | Emissor, validado na leitura |
| `iat` | `1790000000` | Data de emissão |
| `exp` | `1790001800` | Data de expiração |

O payload de um JWT é apenas codificado em Base64URL, não criptografado. Por isso o token não carrega senha, hash nem qualquer dado além do necessário para identificar e autorizar.

### Política de expiração: 30 minutos

- O token é autocontido: a API não consulta o banco para validá-lo. Isso significa que excluir um usuário ou mudar o perfil dele só surte efeito no token atual quando ele expira. Trinta minutos limita essa janela.
- É também a janela de uso de um token vazado. Uma hora ou 24 horas ampliariam a exposição sem ganho real, já que refazer login é barato.
- Não há refresh token no escopo. Com ele, o access token poderia cair para 5 a 15 minutos, com o refresh rotacionado e revogável no servidor.
- O valor é configurável por `JWT_TTL`.

## 3. Controle de acesso (RBAC)

### Matriz de permissões

| Operação | ADMIN | OPERATOR | CLIENT |
|---|---|---|---|
| Listar usuários | sim | sim | não |
| Consultar qualquer usuário | sim | sim | só o próprio |
| Consultar o próprio cadastro (`/me`) | sim | sim | sim |
| Criar usuário | sim | não | não |
| Alterar nome e e-mail | qualquer usuário | clientes, outros operadores e a si mesmo | não |
| Alterar perfil de acesso | sim | não | não |
| Excluir usuário | sim, exceto a própria conta | não | não |

### Onde as regras são aplicadas

1. **Autenticação obrigatória.** `SecurityConfig` libera apenas `POST /api/auth/login`; qualquer outra rota sem token válido recebe `401`.
2. **Regras por perfil, no controller.** Cada método do `UserController` declara `@PreAuthorize`, por exemplo `hasRole('ADMIN')` no POST e no DELETE, e `hasAnyRole('ADMIN','OPERATOR') or @access.isSelf(authentication, #id)` no GET por ID. A checagem de propriedade compara o `sub` do token com o `{id}` da URL, o que impede um cliente de ler outro usuário trocando o ID (IDOR).
3. **Regras que dependem do recurso, no serviço.** `UserService.update` bloqueia com `403` quando um operador tenta alterar uma conta de administrador ou mudar o perfil de alguém, incluindo o próprio. Sem isso, um operador conseguiria se promover a ADMIN via PUT.
4. **Integridade.** `DELETE` da própria conta pelo administrador retorna `409`, evitando que o sistema fique sem administrador por engano.

O frontend esconde botões e rotas conforme o perfil apenas por usabilidade. A decisão de acesso é sempre do backend, e o painel "Respostas da API" na interface mostra os `403` quando uma operação é negada.

## 4. OAuth 2.0 para aplicações parceiras

Hoje a API emite seus próprios tokens a partir de e-mail e senha. Para que uma aplicação parceira acesse dados de um usuário sem receber a senha dele, a arquitetura evolui para OAuth 2.0 com três papéis:

| Papel | Nesta solução |
|---|---|
| Resource Owner | O usuário cadastrado |
| Client | A aplicação parceira, registrada com `client_id`, `client_secret` e URIs de redirecionamento |
| Authorization Server | Serviço dedicado (Spring Authorization Server, Keycloak ou similar) que passa a fazer o login |
| Resource Server | Esta API, que só valida tokens |

### Concessão de acesso (Authorization Code com PKCE)

1. O parceiro redireciona o usuário para `/oauth2/authorize` do Authorization Server com `client_id`, `redirect_uri`, `scope` (por exemplo `usuarios:read`), `state` e `code_challenge`.
2. O usuário faz login **no Authorization Server**, nunca no parceiro, e vê uma tela de consentimento com as permissões pedidas.
3. Ao aprovar, o Authorization Server redireciona para a `redirect_uri` com um `code` de uso único e curta duração.
4. O backend do parceiro troca o `code`, o `code_verifier` e suas credenciais de cliente por um `access_token` (e opcionalmente um `refresh_token`) em `/oauth2/token`.

Para integrações sistema a sistema, sem usuário envolvido, o parceiro usa o fluxo **Client Credentials**, com escopos concedidos ao próprio cliente.

### Utilização dos tokens

- O parceiro chama a API com `Authorization: Bearer <access_token>`, exatamente como o frontend faz hoje.
- A API valida o token com a chave pública do Authorization Server (JWKS, assinatura RS256 ou ES256), verificando `iss`, `aud`, `exp` e os escopos. A mudança no código é trocar o `JwtDecoder` baseado em segredo por `spring.security.oauth2.resourceserver.jwt.issuer-uri`; o filtro e as anotações `@PreAuthorize` continuam os mesmos, acrescidos de checagem de escopo como `hasAuthority('SCOPE_usuarios:read')`.
- O acesso efetivo é a interseção entre o que o escopo permite e o que o perfil do usuário permite. Um parceiro com `usuarios:read` agindo em nome de um CLIENT continua vendo apenas os dados desse cliente.
- Quando o access token expira, o parceiro usa o refresh token. O usuário pode revogar o consentimento a qualquer momento, invalidando os tokens do parceiro.

### Benefícios

- **Sem compartilhamento de senha.** O parceiro nunca vê a credencial do usuário, então um vazamento no parceiro não compromete a conta.
- **Delegação com escopo mínimo.** O parceiro recebe só as permissões que pediu e que o usuário aprovou, em vez de acesso total à conta.
- **Revogação independente.** Cortar o acesso de um parceiro não exige trocar a senha do usuário nem afeta outras integrações.
- **Tokens de vida curta e auditáveis.** Cada token identifica o cliente (`client_id`/`azp`), o que permite rastrear e limitar o uso por parceiro.
- **Separação de responsabilidades.** Login, MFA e política de senha ficam centralizados no Authorization Server; a API só valida tokens.

## 5. Análise de segurança

| # | Risco | Mitigação | Status |
|---|---|---|---|
| 1 | **Roubo de token JWT** (tráfego interceptado, XSS, log) | Validade de 30 minutos; token em `sessionStorage` (some ao fechar a aba) e nunca em URL; o painel da interface exibe o token truncado; CSP restritiva no frontend (`default-src 'self'`, `connect-src` só para a API) reduz a superfície de XSS; em produção, TLS obrigatório | Implementado, exceto TLS |
| 2 | **Vazamento de senhas** | BCrypt com salt por usuário; hash nunca serializado nas respostas; senha limitada a 72 bytes (limite do BCrypt, evitando truncamento silencioso) e mínimo de 8 caracteres | Implementado |
| 3 | **Acesso indevido a dados de outros usuários (IDOR) e escalada de privilégio** | `@PreAuthorize` em todos os endpoints; checagem de propriedade pelo `sub` do token; operador impedido de alterar perfis e contas de administrador | Implementado |
| 4 | **Falsificação de token** | Assinatura HS256 validada em toda requisição; algoritmo fixado (rejeita `alg: none` e troca de algoritmo); emissor validado; segredo mínimo de 32 bytes exigido na inicialização e injetado por variável de ambiente | Implementado |
| 5 | **Enumeração de usuários no login** | Mesma resposta e tempo equivalente para e-mail inexistente e senha errada | Implementado |
| 6 | **Injeção de SQL** | Acesso a dados apenas por Spring Data JPA com parâmetros vinculados; nenhuma query concatenada | Implementado |
| 7 | **Vazamento de detalhes internos em erros** | Respostas de erro padronizadas; `server.error.include-message` e `include-stacktrace` desligados | Implementado |
| 8 | **Força bruta no login** | Rate limiting por IP e por conta (por exemplo Bucket4j na API ou limite no gateway) e bloqueio temporário após falhas consecutivas | Recomendado |
| 9 | **Token válido após exclusão ou rebaixamento de perfil** | Hoje limitado pela validade de 30 minutos. Para revogação imediata: lista de `jti` revogados ou versão de token por usuário conferida no decoder | Recomendado |

### Notas sobre CSRF e CORS

CSRF está desabilitado conscientemente: o token vai no header `Authorization`, que o navegador não envia sozinho em requisições forjadas por outro site. Se o token migrar para cookie, a proteção CSRF precisa ser religada. CORS está restrito às origens de `CORS_ALLOWED_ORIGINS` (padrão `http://localhost:5173`), aos métodos `GET`, `POST`, `PUT` e `DELETE` e aos headers `Authorization` e `Content-Type`; preflight de qualquer outra origem recebe `403`. CORS só protege chamadas feitas por navegador: parceiros que chamam servidor a servidor não passam por ele, por isso a proteção real continua sendo o token.
