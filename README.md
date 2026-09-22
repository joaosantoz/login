# Gestão de Usuários: API REST segura

## Objetivo

API REST para cadastro, consulta, atualização e exclusão de usuários, pensada para integração com aplicações parceiras. O projeto aplica na prática autenticação com JWT, autorização por perfil (RBAC: Administrador, Operador e Cliente) e boas práticas de segurança em aplicações web. Uma interface em Vue.js permite usar e demonstrar todos os recursos da API, exibindo as respostas de cada chamada.

| Documento | Conteúdo |
|---|---|
| [`docs/API.md`](docs/API.md) | Endpoints, métodos HTTP, códigos de resposta, perfis de acesso, JWT, OAuth 2.0 e análise de segurança |
| [`docs/evidencias/`](docs/evidencias/) | Capturas de tela, vídeo de demonstração e saída do teste da API |

Repositório: https://github.com/joaosantoz/login

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Security 7 (OAuth2 Resource Server), Spring Data JPA, Bean Validation, Liquibase |
| Banco | PostgreSQL 17 |
| Frontend | Vue 3, Vue Router, Vite |
| Infra | Docker, Docker Compose (um Dockerfile para o backend e outro para o frontend) |

## Como instalar

Pré-requisitos: Docker e Docker Compose v2. Nenhuma outra instalação é necessária, pois Java, Node e PostgreSQL rodam dentro dos containers.

```bash
git clone https://github.com/joaosantoz/login.git
cd login
```

Ou, a partir do arquivo entregue: `unzip gestao-usuarios.zip && cd gestao-usuarios`.

As portas 5173, 8080 e 5432 precisam estar livres.

## Como executar

```bash
docker compose up --build
```

A primeira execução baixa as imagens e dependências e leva alguns minutos. As seguintes sobem em segundos.

| Serviço | Endereço |
|---|---|
| Frontend | http://localhost:5173 |
| API | http://localhost:8080/api |
| PostgreSQL | `localhost:5432`, banco `sistema`, usuário `sistema`, senha `sistema` |

A ordem de subida é garantida por healthchecks: `db` saudável, depois `backend` aceitando conexões, depois `frontend`.

Para parar e apagar os dados:

```bash
docker compose down -v
```

## Como testar

### Pela interface

1. Acesse http://localhost:5173.
2. Clique em uma das contas de demonstração (o formulário é preenchido) e em **Entrar**.
3. O painel **Respostas da API**, à direita, mostra método, rota, status e corpo de cada chamada.

Roteiro sugerido:

| Perfil | O que verificar |
|---|---|
| Administrador | Criar usuário (e ver a validação ao salvar em branco), editar, alterar perfil, excluir com confirmação |
| Operador | Listar e editar dados de um cliente; o perfil fica bloqueado e não há botão de cadastro nem de exclusão |
| Cliente | Ver o próprio cadastro e as claims do JWT; em **Testar permissões**, a listagem retorna `403` |

### Pela API

Com a aplicação no ar, o script abaixo percorre a matriz de permissões com as três contas e confere o código HTTP de cada caso (login inválido, token adulterado, IDOR, escalada de privilégio, validação, conflito de e-mail, exclusão):

```bash
./scripts/testar-api.sh
```

Saída esperada: `24 passaram, 0 falharam`. Exemplos de chamadas individuais com `curl` estão em [`docs/API.md`](docs/API.md#exemplos).

## Evidências de funcionamento

Todas em [`docs/evidencias/`](docs/evidencias/):

- `demonstracao.mp4`: vídeo do fluxo completo com os três perfis.
- `testar-api.txt`: saída do script de teste da API.
- Capturas de tela:

| Arquivo | Mostra |
|---|---|
| `01-login.png` | Tela de login com as contas de demonstração |
| `02-login-credenciais-invalidas.png` | Login recusado com `401` |
| `03-admin-lista-usuarios.png` | Listagem vista pelo administrador |
| `04-admin-cadastro-validacao.png` | Erros de validação por campo (`400`) |
| `05-admin-cadastro-preenchido.png` | Cadastro com escolha de perfil |
| `06-admin-usuario-criado.png` | Usuário criado (`201`) |
| `07-admin-edicao.png` | Edição de nome e perfil |
| `08-admin-confirmar-exclusao.png` | Confirmação de exclusão |
| `09-admin-usuario-excluido.png` | Usuário excluído (`204`) |
| `10-operador-lista-sem-cadastro.png` | Operador sem acesso a cadastro e exclusão |
| `11-operador-edicao-perfil-bloqueado.png` | Operador edita dados, mas não o perfil |
| `12-cliente-perfil-e-token.png` | Cliente vê apenas o próprio cadastro e as claims do token |
| `13-cliente-acesso-negado-403.png` | Cliente tenta listar usuários e recebe `403` |
| `14-cliente-rota-restrita-redireciona.png` | Rota restrita redireciona para o perfil |

## Detalhes de execução

### Contas de demonstração

Criadas na primeira subida com banco vazio (`SEED_ENABLED=true`). Senha de todas: `Senha@123`.

| E-mail | Perfil |
|---|---|
| admin@sistema.local | ADMIN |
| operador@sistema.local | OPERATOR |
| cliente@sistema.local | CLIENT |

### Configuração

Os valores padrão do `compose.yaml` servem para rodar localmente. Para qualquer outro ambiente, copie `.env.example` para `.env` e troque `POSTGRES_PASSWORD` e `JWT_SECRET` (mínimo 32 bytes; a aplicação não sobe com segredo menor).

| Variável | Padrão | Uso |
|---|---|---|
| `JWT_SECRET` | segredo de desenvolvimento | Chave HMAC-SHA256 do token |
| `JWT_TTL` | `30m` | Validade do token |
| `SEED_ENABLED` | `true` no compose, `false` na aplicação | Cria as contas de demonstração |
| `SEED_PASSWORD` | `Senha@123` | Senha das contas de demonstração |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `sistema` | Credenciais do banco |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens do navegador autorizadas a chamar a API (lista separada por vírgula) |
| `VITE_API_URL` | `http://localhost:8080` | Endereço da API usado pelo frontend, fixado no build |

### Desenvolvimento sem container

```bash
docker compose up -d db
cd backend && SEED_ENABLED=true JWT_SECRET=dev-only-secret-change-me-0123456789abcdef ./gradlew bootRun
cd frontend && npm install && npm run dev
```

O frontend sobe em http://localhost:5173, origem já liberada no CORS da API.

## Estrutura

```
backend/
  Dockerfile
  build.gradle.kts
  src/main/java/com/cadastro/sistema/
    config/        SecurityConfig, propriedades de JWT e CORS, carga das contas de demonstração
    controller/    AuthController, UserController
    dto/auth/      LoginRequest, LoginResponse
    dto/user/      CreateUserRequest, UpdateUserRequest, UserResponse
    entity/        User, Role
    exception/     exceções de domínio e ApiExceptionHandler (Problem Details, RFC 9457)
    repository/    UserRepository
    security/      TokenService, AuthenticatedUser, AccessPolicy, TokenClaims
    service/       AuthService, UserService
  src/main/resources/
    application.yaml
    db/changelog/  migrações Liquibase
frontend/
  Dockerfile
  vite.config.js
  src/
    assets/        estilos
    components/    AppHeader, ApiResponsePanel
    constants/     perfis de acesso
    router/        rotas e guarda de navegação por perfil
    services/      cliente HTTP, authService, userService
    stores/        sessão (token e usuário logado)
    views/         LoginView, UserListView, UserFormView, ProfileView
docs/
  API.md
  evidencias/
scripts/
  testar-api.sh
compose.yaml
```

## Revisão das dependências do Spring Initializr

O `build.gradle.kts` gerado foi ajustado assim:

| Dependência | Ação | Motivo |
|---|---|---|
| `spring-boot-starter-webmvc` | mantida | API REST |
| `spring-boot-starter-security` | mantida | Autenticação e autorização |
| `spring-boot-starter-data-jpa` | mantida | Persistência |
| `spring-boot-starter-liquibase` | mantida | Schema versionado; Hibernate só valida (`ddl-auto: validate`) |
| `postgresql` | mantida | Driver do banco usado |
| `spring-boot-devtools` | mantida | Restart automático em desenvolvimento, não entra no jar |
| `spring-boot-starter-security-oauth2-resource-server` | adicionada | Emissão (`JwtEncoder`) e validação (`JwtDecoder`) de JWT com Nimbus, integradas ao Spring Security. Dispensa biblioteca JWT de terceiros e filtro caseiro |
| `spring-boot-starter-validation` | adicionada | Validação dos payloads (`@Valid`) |
| `spring-boot-starter-jdbc` | removida | Já vem transitivamente pelo starter de JPA |
| `ojdbc17` | removida | Oracle não é usado |
| `spring-boot-docker-compose` | removida | O próprio compose sobe o backend; a integração tentaria gerenciar o mesmo arquivo |
| starters `*-test` e `junit-platform-launcher` | removidas | Escopo sem testes automatizados |

Toolchain alterado de Java 27 para Java 25: é a LTS atual e tem imagens oficiais `eclipse-temurin:25` para build e runtime.

## Decisões

- **Resource Server do Spring Security em vez de filtro JWT próprio.** A validação de assinatura, expiração e emissor fica a cargo do `BearerTokenAuthenticationFilter`, que já responde `401` com `WWW-Authenticate` padrão. O mesmo mecanismo é o que seria usado se a API passasse a aceitar tokens de um Authorization Server OAuth 2.0 (ver `docs/API.md`).
- **Autorização declarada no controller.** Regras por perfil ficam em `@PreAuthorize`, legíveis junto de cada endpoint. Regras que dependem do estado do recurso (operador não altera administradores nem perfis) ficam no `UserService`, onde o recurso já foi carregado.
- **CORS explícito em vez de proxy.** Frontend (`:5173`) e API (`:8080`) são origens diferentes. O Spring Security libera apenas as origens de `CORS_ALLOWED_ORIGINS`, os métodos usados e os headers `Authorization` e `Content-Type`; qualquer outra origem recebe `403` no preflight.
- **Frontend servido pelo `vite preview`.** O container faz o build e serve os arquivos estáticos com o próprio Vite, com cabeçalhos de segurança (CSP, `X-Frame-Options`, `nosniff`) definidos em `vite.config.js`. Para produção real, o mesmo `dist/` iria para uma CDN ou servidor estático dedicado.
- **Erros no formato Problem Details (RFC 9457).** Todas as respostas de erro têm `status`, `title` e `detail`; erros de validação trazem `errors` por campo.
- **Sem paginação na listagem.** Volume esperado é pequeno para o escopo; quando crescer, `GET /api/usuarios` recebe `page`/`size` sem quebrar o contrato do item.
