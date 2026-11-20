# Help Desk API

API REST para um sistema de gerenciamento de chamados de suporte técnico (help desk), com controle de acesso baseado em papéis e permissões (RBAC), autenticação via JWT, fluxo de senha temporária no cadastro de usuários, relatórios exportáveis e log de auditoria.

Projeto de estudo/portfólio, construído de forma incremental.

## Status do projeto

| Módulo | Status |
|---|---|
| Usuários (cadastro, autenticação, autorização) | ✅ Completo |
| Chamados (tickets) | ✅ Completo |
| Relatórios e métricas (com exportação Excel/PDF) | ✅ Completo |
| Log de auditoria | ✅ Completo |
| Testes automatizados | ✅ Completo |

> O módulo de Denúncias, previsto na especificação original, foi removido do escopo por decisão própria.

## Tecnologias

- **Java 25**
- **Spring Boot 4.1.1**
- **Spring Data JPA** (Hibernate) + **MySQL**
- **Flyway** — controle de schema via migrations versionadas (substituiu `ddl-auto=update`)
- **Spring Security** + **JWT** ([jjwt](https://github.com/jwtk/jjwt))
- **MapStruct** — mapeamento entre entidades e DTOs
- **Lombok**
- **Spring Mail** — envio de e-mail (testado com [Mailtrap](https://mailtrap.io))
- **springdoc-openapi** — documentação interativa (Swagger UI)
- **Spring Boot Actuator** — endpoint de health check
- **Apache POI** / **OpenPDF** — exportação de relatórios em Excel/PDF
- Carregamento de `.env` via um `EnvironmentPostProcessor` próprio (a lib `spring-dotenv` não funciona com o Spring Boot 4.1.1)
- **Docker** / **Docker Compose** — containerização da aplicação + banco
- **JUnit 5** / **Mockito** — testes unitários
- **Testcontainers** — testes de integração contra um MySQL real, descartável
- **GitHub Actions** — pipeline de CI
- **Maven**

## Como funciona

### Autenticação

A API é *stateless*: autenticação via **JWT**, enviado no header `Authorization: Bearer <token>` em toda requisição (exceto login). Não há sessão nem cookie.

### RBAC dinâmico (Roles + Permissions)

- **`Permission`**: um catálogo fixo de permissões (`CREATE_TICKET`, `MANAGE_USERS`, `VIEW_REPORTS`, `VIEW_AUDIT_LOGS`, etc.), definido no código.
- **`Role`**: uma entidade de banco — um "pacote" configurável de permissões (ex: `CUSTOMER`, `TECHNICIAN`, `MANAGER`), atribuída a um usuário.
- Além da role, um usuário também pode receber **permissões isoladas** direto nele, por cima das que a role já concede — cobre exceções pontuais sem precisar criar uma role nova.
- Autorização nos endpoints usa `@PreAuthorize("hasAuthority('...')")`, checando a permissão em si, não o nome da role.

### Cadastro de usuário com senha temporária

Quem cadastra um usuário (um `MANAGER`) não define a senha dele. O sistema:
1. Gera uma senha temporária aleatória.
2. Salva o usuário com essa senha (já criptografada com BCrypt).
3. Envia a senha temporária por e-mail para o usuário recém-criado (uma falha no envio não derruba o cadastro — a senha fica logada no servidor como rede de segurança).
4. Marca o usuário como "deve trocar a senha" — qualquer requisição autenticada, exceto a própria troca de senha, é bloqueada até ele trocar.

### Ciclo de vida do chamado (Ticket)

Um chamado passa pelos estados `PENDING` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, nessa ordem estrita (não dá pra pular etapa). `CANCELLED` só é possível a partir de `PENDING`, e reabrir (`reopen`) volta de `RESOLVED`/`CLOSED` pra `IN_PROGRESS`, mantendo o mesmo técnico.

Cada ação (criar, assumir, resolver, fechar, cancelar, reabrir, editar) é um endpoint dedicado — não um `PUT` genérico recebendo um status qualquer — e passa por duas camadas de autorização:
1. **Permissão** (`@PreAuthorize`): o usuário tem a permissão necessária pra essa ação (ex: `RESOLVE_TICKET`)?
2. **Dono do registro** (`TicketValidator`): é especificamente o cliente dono *deste* chamado, ou o técnico responsável *por este* chamado? Essa checagem não dá pra fazer só com `@PreAuthorize`, porque depende do dado concreto sendo acessado, não só do papel do usuário — sem ela, qualquer cliente com a permissão `VIEW_OWN_TICKETS` conseguiria ver o chamado de qualquer outro cliente.

### Relatórios e métricas

Relatórios são calculados dinamicamente a partir dos chamados, filtrados por período (`startDate`/`endDate`, ambos opcionais e independentes) e, opcionalmente, por setor/tema/status. O filtro de data vale sobre a **data de criação** do chamado — todas as métricas do relatório são calculadas sobre esse mesmo grupo, pra ficarem comparáveis entre si.

Métricas incluídas: total criado/atendido/cancelado, tempo médio de finalização, chamados por setor, chamados por tema, tema e setor mais/menos solicitados. O relatório pode ser consultado em JSON ou **exportado em Excel (.xlsx) ou PDF**.

### Log de auditoria

Diferente de `createdBy`/`updatedBy` (que só guardam a **última** pessoa a mexer num registro), o log de auditoria é um **histórico completo e imutável**: cada ação relevante sobre um chamado (criado, editado, assumido, resolvido, fechado, cancelado, reaberto) ou usuário (criado, atualizado, desativado, permissão concedida/revogada) gera uma linha nova, nenhuma é sobrescrita. Consultável via `GET /api/v1/audit-logs`, com permissão `VIEW_AUDIT_LOGS`.

### Auditoria automática e soft delete

Toda entidade registra automaticamente quem criou/atualizou e quando (`createdBy`/`createdOn`/`updatedBy`/`updatedOn`, via Spring Data Auditing). Exclusão de usuário é sempre **soft delete**: o registro nunca é removido do banco, só marcado como inativo.

O e-mail de um usuário desativado permanece **permanentemente** amarrado àquele registro — não existe cadastro novo com o mesmo e-mail de alguém já desativado (a constraint `UNIQUE` não distingue ativo/inativo, de propósito). Decisão intencional pensando num cenário corporativo: reaproveitar o e-mail de alguém desligado para uma pessoa diferente misturaria o histórico (chamados, auditoria, ações) das duas pessoas sob a mesma identidade. Se a pessoa original voltar, o caminho é **reativar o mesmo registro** (`POST /api/v1/users/{id}/reactivate`), nunca criar um usuário novo com aquele e-mail.

### Seed inicial

Na primeira vez que a aplicação sobe com o banco vazio, são criados automaticamente: as roles padrão (`CUSTOMER`, `TECHNICIAN`, `MANAGER`, com as permissões correspondentes) e um usuário `MANAGER` inicial, com senha temporária impressa no console — necessário pra poder logar e começar a cadastrar os demais usuários.

### Segurança

- **SQL Injection**: não é um risco nesse projeto — toda consulta passa pelo Spring Data JPA (métodos derivados) ou pela Specification API/CriteriaBuilder (`TicketSpecs`, `UserSpecs`, `AuditLogSpecs`), que sempre usam parâmetros vinculados, nunca concatenação de string.
- **CORS**: origens permitidas configuráveis via `.env` (`CORS_ALLOWED_ORIGINS`), nunca `*`.
- **Força bruta no login**: após 5 tentativas erradas seguidas pro mesmo e-mail, bloqueia novas tentativas por 15 minutos (429), mesmo com a senha certa.
- **Vazamento de senha**: o hash da senha nunca é serializado em resposta (`@JsonIgnore`) nem aparece em log via `toString()` (`@ToString.Exclude`).
- **Erros HTTP consistentes**: todo erro (validação, duplicidade, não encontrado, sem permissão, etc.) usa o mesmo formato de resposta (`ErrorResponse`), com o status HTTP correto — nada de 500 genérico pra erro de autorização ou 404 com corpo vazio.
- **Paginação protegida**: `pageSize` tem um teto (100) pra evitar que um cliente force a aplicação a carregar a tabela inteira de uma vez.

### Documentação interativa (Swagger)

Com a aplicação rodando, acesse:
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` — testa os endpoints direto do navegador (botão "Authorize" pra colar o JWT).
- **JSON da especificação OpenAPI**: `http://localhost:8080/v3/api-docs`.

### Versionamento de API

Todos os endpoints de negócio vivem sob o prefixo `/api/v1`, aplicado automaticamente a todos os controllers (não precisa repetir em cada um). Endpoints de infraestrutura (Swagger, Actuator) ficam fora desse prefixo, por convenção.

### Testes automatizados

Dois tipos, com propósitos diferentes:

- **Unitários** (JUnit 5 + Mockito) — testam uma única classe isolada, sem banco, sem Spring, sem rede. Cobrem os Validators (`TicketValidator`, `UserValidator`), onde moram as regras de negócio. Dependências externas (ex: `UserRepository`, no caso do `UserValidator`) são "fingidas" com Mockito, pra manter o teste rápido e focado só na lógica da classe.
- **De integração** (`@SpringBootTest` + `MockMvc` + **Testcontainers**) — sobem a aplicação inteira contra um **MySQL real**, rodando num container Docker descartável, e simulam requisições HTTP de verdade (Controller → Service → Validator → banco), incluindo segurança, validação e os códigos de status HTTP. Cobrem os fluxos principais: login (sucesso, senha errada, bloqueio por força bruta), CRUD de usuário (conflito de duplicidade, 404, desativar/reativar) e o ciclo de vida do chamado (criar → assumir → resolver → fechar, com as checagens de permissão e de dono).

Rodar localmente: `./mvnw test` (exige o **Docker rodando**, por causa do Testcontainers) ou `./mvnw clean verify` (mesmo comando usado no CI).

## Endpoints

### Autenticação (`/api/v1/auth`)

| Método | Rota | Descrição | Autenticação |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Login (e-mail + senha), devolve o JWT. Bloqueia por 429 após 5 tentativas erradas seguidas | Pública |
| PUT | `/api/v1/auth/change-password` | Troca a senha do usuário autenticado | Obrigatória |

### Usuários (`/api/v1/users`)

Todos exigem a permissão `MANAGE_USERS`, exceto a reativação, que tem permissão própria (`REACTIVATE_USER`).

| Método | Rota | Descrição | Permissão |
|---|---|---|---|
| POST | `/api/v1/users` | Cadastra um novo usuário | `MANAGE_USERS` |
| GET | `/api/v1/users/{id}` | Busca um usuário por id | `MANAGE_USERS` |
| GET | `/api/v1/users` | Busca paginada, com filtros opcionais `firstName`, `lastName`, `department` (múltiplo), `page`, `pageSize` (máx. 100) | `MANAGE_USERS` |
| PUT | `/api/v1/users/{id}` | Atualiza um usuário | `MANAGE_USERS` |
| DELETE | `/api/v1/users/{id}` | Desativa um usuário (soft delete) | `MANAGE_USERS` |
| POST | `/api/v1/users/{id}/reactivate` | Reativa um usuário desativado (mesmo registro, mesmo e-mail e histórico) | `REACTIVATE_USER` |
| POST | `/api/v1/users/{id}/permissions/{permission}` | Concede uma permissão isolada ao usuário | `MANAGE_USERS` |
| DELETE | `/api/v1/users/{id}/permissions/{permission}` | Remove uma permissão isolada do usuário | `MANAGE_USERS` |

### Chamados (`/api/v1/tickets`)

Cada rota exige uma permissão específica e, nas ações sobre um chamado já existente, também passa pela checagem de dono/técnico responsável (ver [Ciclo de vida do chamado](#ciclo-de-vida-do-chamado-ticket)).

| Método | Rota | Descrição | Permissão |
|---|---|---|---|
| POST | `/api/v1/tickets` | Cria um chamado (o cliente autenticado é sempre o dono) | `CREATE_TICKET` |
| GET | `/api/v1/tickets/{id}` | Busca um chamado por id | `VIEW_OWN_TICKETS`, `VIEW_PENDING_TICKETS` ou `VIEW_REPORTS` |
| GET | `/api/v1/tickets` | Busca paginada — cliente só vê os próprios, técnico vê `PENDING` + os seus, gerente vê todos. Filtros opcionais `status`, `theme`, `page`, `pageSize` (máx. 100) | idem |
| PUT | `/api/v1/tickets/{id}` | Edita a descrição (só enquanto `PENDING`, só o dono) | `CREATE_TICKET` |
| POST | `/api/v1/tickets/{id}/assume` | Técnico assume o chamado (`PENDING` → `IN_PROGRESS`) | `ASSUME_TICKET` |
| POST | `/api/v1/tickets/{id}/resolve` | Técnico responsável resolve (`IN_PROGRESS` → `RESOLVED`) | `RESOLVE_TICKET` |
| POST | `/api/v1/tickets/{id}/close` | Cliente confirma e fecha (`RESOLVED` → `CLOSED`) | `CLOSE_TICKET` |
| POST | `/api/v1/tickets/{id}/cancel` | Cliente cancela (só a partir de `PENDING`) | `CANCEL_TICKET` |
| POST | `/api/v1/tickets/{id}/reopen` | Cliente reabre (`RESOLVED`/`CLOSED` → `IN_PROGRESS`) | `REOPEN_TICKET` |

### Relatórios (`/api/v1/reports`)

Todos exigem a permissão `VIEW_REPORTS`. Filtros `startDate`, `endDate`, `department`, `theme`, `status` são todos opcionais e combináveis.

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/v1/reports` | Relatório em JSON |
| GET | `/api/v1/reports/export/excel` | Relatório em planilha `.xlsx` |
| GET | `/api/v1/reports/export/pdf` | Relatório em `.pdf` |

### Log de auditoria (`/api/v1/audit-logs`)

| Método | Rota | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/audit-logs` | Busca paginada, com filtros opcionais `entityType`, `action`, `startDate`, `endDate`, `page`, `pageSize` (máx. 100) | `VIEW_AUDIT_LOGS` |

### Infraestrutura

| Método | Rota | Descrição |
|---|---|---|
| GET | `/actuator/health` | Health check (público) |
| GET | `/swagger-ui/index.html` | Documentação interativa |
| GET | `/v3/api-docs` | Especificação OpenAPI em JSON |

## Como rodar o projeto

### Opção A — Docker Compose (recomendado)

Sobe a aplicação **e** o MySQL juntos, sem precisar instalar nada além do Docker.

1. Clone o repositório.
2. Copie `.env.example` para `.env` e preencha com os valores reais de e-mail (Mailtrap) e um `JWT_SECRET` aleatório próprio — as credenciais do banco, nesse modo, já vêm definidas no `docker-compose.yml`.
3. Suba tudo:
   ```
   docker compose up --build
   ```
4. No log do container `app`, procure pelo usuário `MANAGER` inicial (e-mail + senha temporária).

### Opção B — Local (Maven + MySQL instalado na máquina)

#### Pré-requisitos

- Java 25
- MySQL rodando localmente
- Uma conta de teste no [Mailtrap](https://mailtrap.io) (Email Testing / sandbox) — gratuita

#### Passo a passo

1. Clone o repositório.
2. Copie `.env.example` para `.env` e preencha com os valores reais (credenciais do MySQL, do Mailtrap, origens de CORS, e gere um `JWT_SECRET` aleatório próprio):
   ```
   cp .env.example .env
   ```
3. Rode a aplicação:
   ```
   ./mvnw spring-boot:run
   ```
   O banco (`help_desk`) é criado automaticamente na primeira conexão, e o schema é montado pelas migrations do Flyway.
4. No console, procure pelo log do usuário `MANAGER` inicial (e-mail + senha temporária) — é com ele que você faz o primeiro login.
5. Faça login em `POST /api/v1/auth/login` e troque a senha em `PUT /api/v1/auth/change-password` antes de usar os outros endpoints.

## CI

Todo push/PR pra `main` roda automaticamente (`.github/workflows/ci.yml`): sobe um MySQL de serviço e executa `mvn clean verify`, rodando a suíte de testes completa (unitários + integração) a cada mudança.
