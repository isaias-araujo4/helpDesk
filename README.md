# Help Desk API

API REST para um sistema de gerenciamento de chamados de suporte técnico (help desk), com controle de acesso baseado em papéis e permissões (RBAC), autenticação via JWT e fluxo de senha temporária no cadastro de usuários.

Projeto de estudo/portfólio, construído de forma incremental.

## Status do projeto

| Módulo | Status |
|---|---|
| Usuários (cadastro, autenticação, autorização) | ✅ Completo |
| Chamados (tickets) | 🚧 Planejado |
| Denúncias | 🚧 Planejado |
| Relatórios e métricas | 🚧 Planejado |

## Tecnologias

- **Java 25**
- **Spring Boot 4.1.1**
- **Spring Data JPA** (Hibernate) + **MySQL**
- **Spring Security** + **JWT** ([jjwt](https://github.com/jwtk/jjwt))
- **MapStruct** — mapeamento entre entidades e DTOs
- **Lombok**
- **Spring Mail** — envio de e-mail (testado com [Mailtrap](https://mailtrap.io))
- **spring-dotenv** — carrega configuração sensível de um arquivo `.env`
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
