# Help Desk API

API REST para um sistema de gerenciamento de chamados de suporte técnico (help desk), com controle de acesso baseado em papéis e permissões (RBAC), autenticação via JWT e fluxo de senha temporária no cadastro de usuários.

Projeto de estudo/portfólio, construído de forma incremental.

## Status do projeto

| Módulo | Status |
|---|---|
| Usuários (cadastro, autenticação, autorização) | ✅ Completo |
| Chamados (tickets) | ✅ Completo |
| Relatórios e métricas | 🚧 Planejado |

## Tecnologias

- **Java 25**
- **Spring Boot 4.1.1**
- **Spring Data JPA** (Hibernate) + **MySQL**
- **Spring Security** + **JWT** ([jjwt](https://github.com/jwtk/jjwt))
- **MapStruct** — mapeamento entre entidades e DTOs
- **Lombok**
- **Spring Mail** — envio de e-mail (testado com [Mailtrap](https://mailtrap.io))
- Carregamento de `.env` via um `EnvironmentPostProcessor` próprio (a lib `spring-dotenv` não funciona com o Spring Boot 4.1.1)
- **Maven**

## Como funciona

### Autenticação

A API é *stateless*: autenticação via **JWT**, enviado no header `Authorization: Bearer <token>` em toda requisição (exceto login). Não há sessão nem cookie.

### RBAC dinâmico (Roles + Permissions)

- **`Permission`**: um catálogo fixo de permissões (`CREATE_TICKET`, `MANAGE_USERS`, `VIEW_REPORTS`, etc.), definido no código.
- **`Role`**: uma entidade de banco — um "pacote" configurável de permissões (ex: `CUSTOMER`, `TECHNICIAN`, `MANAGER`), atribuída a um usuário.
- Além da role, um usuário também pode receber **permissões isoladas** direto nele, por cima das que a role já concede — cobre exceções pontuais sem precisar criar uma role nova.
- Autorização nos endpoints usa `@PreAuthorize("hasAuthority('...')")`, checando a permissão em si, não o nome da role.

### Cadastro de usuário com senha temporária

Quem cadastra um usuário (um `MANAGER`) não define a senha dele. O sistema:
1. Gera uma senha temporária aleatória.
2. Salva o usuário com essa senha (já criptografada com BCrypt).
3. Envia a senha temporária por e-mail para o usuário recém-criado.
4. Marca o usuário como "deve trocar a senha" — qualquer requisição autenticada, exceto a própria troca de senha, é bloqueada até ele trocar.

### Auditoria e soft delete

Toda entidade registra automaticamente quem criou/atualizou e quando (`createdBy`/`createdOn`/`updatedBy`/`updatedOn`, via Spring Data Auditing). Exclusão de usuário é sempre **soft delete**: o registro nunca é removido do banco, só marcado como inativo.

### Seed inicial

Na primeira vez que a aplicação sobe com o banco vazio, são criados automaticamente: as roles padrão (`CUSTOMER`, `TECHNICIAN`, `MANAGER`, com as permissões correspondentes) e um usuário `MANAGER` inicial, com senha temporária impressa no console — necessário pra poder logar e começar a cadastrar os demais usuários.

### Ciclo de vida do chamado (Ticket)

Um chamado passa pelos estados `PENDING` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, nessa ordem estrita (não dá pra pular etapa). `CANCELLED` só é possível a partir de `PENDING`, e reabrir (`reopen`) volta de `RESOLVED`/`CLOSED` pra `IN_PROGRESS`, mantendo o mesmo técnico.

Cada ação (criar, assumir, resolver, fechar, cancelar, reabrir, editar) é um endpoint dedicado — não um `PUT` genérico recebendo um status qualquer — e passa por duas camadas de autorização:
1. **Permissão** (`@PreAuthorize`): o usuário tem a permissão necessária pra essa ação (ex: `RESOLVE_TICKET`)?
2. **Dono do registro** (`TicketValidator`): é especificamente o cliente dono *deste* chamado, ou o técnico responsável *por este* chamado? Essa checagem não dá pra fazer só com `@PreAuthorize`, porque depende do dado concreto sendo acessado, não só do papel do usuário — sem ela, qualquer cliente com a permissão `VIEW_OWN_TICKETS` conseguiria ver o chamado de qualquer outro cliente.

## Endpoints

### Autenticação (`/auth`)

| Método | Rota | Descrição | Autenticação |
|---|---|---|---|
| POST | `/auth/login` | Login (e-mail + senha), devolve o JWT | Pública |
| PUT | `/auth/change-password` | Troca a senha do usuário autenticado | Obrigatória |

### Usuários (`/users`)

Todos exigem a permissão `MANAGE_USERS`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/users` | Cadastra um novo usuário |
| GET | `/users/{id}` | Busca um usuário por id |
| GET | `/users` | Busca paginada, com filtros opcionais `firstName`, `lastName`, `department` (múltiplo), `page`, `pageSize` |
| PUT | `/users/{id}` | Atualiza um usuário |
| DELETE | `/users/{id}` | Desativa um usuário (soft delete) |

### Chamados (`/tickets`)

Cada rota exige uma permissão específica e, nas ações sobre um chamado já existente, também passa pela checagem de dono/técnico responsável (ver [Ciclo de vida do chamado](#ciclo-de-vida-do-chamado-ticket)).

| Método | Rota | Descrição | Permissão |
|---|---|---|---|
| POST | `/tickets` | Cria um chamado (o cliente autenticado é sempre o dono) | `CREATE_TICKET` |
| GET | `/tickets/{id}` | Busca um chamado por id | `VIEW_OWN_TICKETS`, `VIEW_PENDING_TICKETS` ou `VIEW_REPORTS` |
| GET | `/tickets` | Busca paginada — cliente só vê os próprios, técnico vê `PENDING` + os seus, gerente vê todos. Filtros opcionais `status`, `theme`, `page`, `pageSize` | idem |
| PUT | `/tickets/{id}` | Edita a descrição (só enquanto `PENDING`, só o dono) | `CREATE_TICKET` |
| POST | `/tickets/{id}/assume` | Técnico assume o chamado (`PENDING` → `IN_PROGRESS`) | `ASSUME_TICKET` |
| POST | `/tickets/{id}/resolve` | Técnico responsável resolve (`IN_PROGRESS` → `RESOLVED`) | `RESOLVE_TICKET` |
| POST | `/tickets/{id}/close` | Cliente confirma e fecha (`RESOLVED` → `CLOSED`) | `CLOSE_TICKET` |
| POST | `/tickets/{id}/cancel` | Cliente cancela (só a partir de `PENDING`) | `CANCEL_TICKET` |
| POST | `/tickets/{id}/reopen` | Cliente reabre (`RESOLVED`/`CLOSED` → `IN_PROGRESS`) | `REOPEN_TICKET` |

## Como rodar o projeto

### Pré-requisitos

- Java 25
- MySQL rodando localmente
- Uma conta de teste no [Mailtrap](https://mailtrap.io) (Email Testing / sandbox) — gratuita

### Passo a passo

1. Clone o repositório.
2. Copie `.env.example` para `.env` e preencha com os valores reais (credenciais do MySQL, do Mailtrap, e gere um `JWT_SECRET` aleatório próprio):
   ```
   cp .env.example .env
   ```
3. Rode a aplicação:
   ```
   ./mvnw spring-boot:run
   ```
   O banco (`help_desk`) é criado automaticamente na primeira conexão, caso ainda não exista.
4. No console, procure pelo log do usuário `MANAGER` inicial (e-mail + senha temporária) — é com ele que você faz o primeiro login.
5. Faça login em `POST /auth/login` e troque a senha em `PUT /auth/change-password` antes de usar os outros endpoints.
