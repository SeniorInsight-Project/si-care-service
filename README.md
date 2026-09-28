# SeniorInsight - Backend Core

O **backend-core** é o serviço central da plataforma de saúde **SeniorInsight**, desenvolvido em **Java 21** e **Spring Boot 3.5**, responsável pela lógica de negócios, segurança, integrações e persistência de dados.

---

## 🛠️ Tecnologias e Ferramentas

- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.5.6 (Web, Data JPA, Data Redis, Validation, Actuator)
- **Gerenciador de Dependências e Build:** Apache Maven
- **Banco de Dados Relacional:** PostgreSQL 17
- **Versionamento de Banco de Dados:** Flyway Migration
- **Cache & Rate Limiting:** Redis 7 (Alpine)
- **Contêineres:** Docker & Docker Compose (Multi-stage build)
- **Nuvem & Hospedagem:** Heroku (Heroku-24 Stack com Java Buildpack)

---

## 📁 Estrutura do Repositório

```text
backend-core/
├── .github/                      # Configurações do GitHub (CODEOWNERS, workflows)
├── src/
│   ├── main/
│   │   ├── java/com/seniorinsight/backendcore/
│   │   │   ├── BackendCoreApplication.java   # Classe principal Spring Boot
│   │   │   ├── HealthController.java         # Endpoints de status (/, /api/v1/health)
│   │   │   ├── config/
│   │   │   │   └── SecurityHeadersFilter.java # Filtro de segurança HTTP (OWASP)
│   │   │   └── security/
│   │   │       └── RateLimiterService.java   # Serviço de Rate Limiting com Redis
│   │   └── resources/
│   │       ├── application.yml               # Configurações comuns e profile padrão (dev)
│   │       ├── application-dev.yml           # Perfil local (Docker Compose / Localhost)
│   │       ├── application-prod.yml          # Perfil de produção (Heroku, SSL, Hikari)
│   │       └── db/migration/
│   │           └── V1__Create_Auth_Tables.sql # Migração Flyway: tabelas do Épico 1
│   └── test/java/com/seniorinsight/backendcore/
│       └── BackendCoreApplicationTests.java  # Testes de contexto e integração
├── Dockerfile                    # Multi-stage build (Maven JDK 21 -> JRE 21 Alpine)
├── docker-compose.yml            # Orquestração local (postgres, redis, backend-core)
├── template.env                  # Template oficial de variáveis de ambiente para dev
├── .env.example                  # Exemplo simplificado de variáveis de ambiente
├── .gitignore                    # Arquivos ignorados pelo Git (inclui .env e /target/)
├── pom.xml                       # Dependências e plugins do Maven
└── README.md                     # Documentação do projeto
```

---

## 🚀 Como Desenvolver e Rodar Localmente

O projeto oferece **dois modos de execução local**, projetados para que qualquer desenvolvedor rode sem esforço.

### Pré-requisitos
- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/) instalados;
- *(Opcional para o Modo 1)* [JDK 21](https://adoptium.net/) e [Maven](https://maven.apache.org/) instalados no host.

---

### Passo 0: Preparar as Variáveis de Ambiente

Antes de iniciar qualquer um dos modos, copie o template de variáveis de ambiente na raiz do projeto:

```bash
cp template.env .env
```

*(O `docker-compose.yml` e o perfil `dev` já possuem valores padrão para todas as variáveis, funcionando direto da caixa).*

---

### Modo 1: Desenvolvimento Ágil na IDE (Recomendado para o dia a dia)
*Neste modo, o Docker gerencia apenas o banco PostgreSQL e o Redis. A aplicação Spring Boot roda diretamente na sua IDE (IntelliJ, VS Code, Eclipse) ou via terminal, permitindo hot-reload, breakpoints e debug rápido.*

1. **Suba os serviços de banco e cache em segundo plano:**
   ```bash
   docker compose up -d postgres redis
   ```

2. **Execute a aplicação na sua máquina:**
   - **Pela IDE:** Execute a classe principal `BackendCoreApplication.java`.
   - **Pelo Terminal:**
     ```bash
     mvn spring-boot:run
     ```

3. **Por que funciona sem mexer em nada?**
   O arquivo `application-dev.yml` possui fallback automático para `localhost`. Quando executada fora do contêiner, a aplicação conecta automaticamente em `localhost:5432` (PostgreSQL) e `localhost:6379` (Redis).

---

### Modo 2: Ambiente 100% Contêiner (Docker Compose)
*Ideal para validar o build de contêiner ou para desenvolvedores que não possuem Java/Maven instalados no sistema.*

1. **Suba toda a infraestrutura:**
   ```bash
   docker compose up --build
   ```

2. **Acompanhe os logs:**
   O contêiner do `backend-core` aguarda os healthchecks de `postgres` e `redis` estarem prontos antes de iniciar.

3. **Parar os serviços:**
   ```bash
   docker compose down          # Mantém os dados persistidos no volume
   docker compose down -v       # Remove contêineres e limpa os volumes do banco
   ```

---

## 🔍 Verificação de Saúde da Aplicação

Com a aplicação em execução (localmente na porta `8080` ou em produção):

| Endpoint | Descrição | Resposta Esperada |
| :--- | :--- | :--- |
| `GET /` | Status amigável da API | `{"service":"SeniorInsight Backend Core","status":"ONLINE",...}` |
| `GET /api/v1/health` | Health Check principal da aplicação | `{"service":"backend-core","status":"UP",...}` |
| `GET /actuator/health` | Health Check do Spring Actuator | `{"status":"UP"}` |

---

## 🗄️ Banco de Dados & Migrações com Flyway

O projeto utiliza o **Flyway** para versionamento e evolução automática do esquema do banco de dados.

- **Local dos scripts SQL:** `src/main/resources/db/migration/`
- **Padrão de nomenclatura:** `V<Número>__<Descricao_Sem_Acentos>.sql` (ex: `V1__Create_Auth_Tables.sql`)
- **Execução:** Ao inicializar a aplicação (seja local ou no Heroku), o Flyway detecta novas migrações e as executa automaticamente.

### Tabelas Criadas na Migração V1 (Épico 1: Acesso e Segurança)
- **`usuarios`**: Gestão de identidade com suporte a UUID v4, login local (e-mail + senha hash), SSO Google/Apple (`provedor_sso`, `sso_id`), status da conta e autenticação em dois fatores (`mfa_habilitado`, `mfa_segredo`).
- **`tokens_recuperacao`**: Tokens descartáveis com expiração temporal para recuperação segura de senha.
- **`flyway_schema_history`**: Tabela interna do Flyway para rastreabilidade de migrações.

---

## 🛡️ Segurança & Hardening Implementados

1. **OWASP Secure Headers:** Filtro global injeta em todas as respostas HTTP:
   - `X-Content-Type-Options: nosniff` (prevenção contra MIME-sniffing)
   - `X-Frame-Options: DENY` (bloqueio de Clickjacking)
   - `Strict-Transport-Security` (HSTS estrito com expiração de 1 ano)
   - `Content-Security-Policy` (`default-src 'self'`)
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - `Permissions-Policy: camera=(), microphone=(), geolocation=()`
2. **Supressão de Whitelabel & Stacktraces:** Em caso de erros (404/500), respostas HTML e rastros de stacktrace são ocultados para evitar Information Disclosure.
3. **Rate Limiting (Redis):** `RateLimiterService` com contadores atômicos e expiração automática para proteger endpoints críticos contra força bruta (Login, MFA, Recuperação de Senha).
4. **Execução Segura em Contêiner:** A imagem final do Docker utiliza a base `eclipse-temurin:21-jre-alpine` rodando com usuário sem privilégios (`spring:spring`).

---

## ☁️ Gestão de Ambientes & Deploy no Heroku

O projeto conta com três perfis principais do Spring:
- **`application.yml`**: Configurações compartilhadas e definição do profile ativo padrão (`${SPRING_PROFILES_ACTIVE:dev}`).
- **`application-dev.yml`**: Conexões locais apontando para contêineres/localhost e variáveis de OAuth locais (`GOOGLE_CLIENT_ID_LOCAL`).
- **`application-prod.yml`**: Perfil de produção para o Heroku, lendo `JDBC_DATABASE_URL` (adaptada automaticamente pelo buildpack da `DATABASE_URL`), `REDIS_URL`, suporte nativo a proxy reverso e credenciais de produção (`GOOGLE_CLIENT_ID_PROD`).

### Config Vars no Heroku (Cadastradas no Painel)
```bash
SPRING_PROFILES_ACTIVE=prod
GOOGLE_CLIENT_ID_PROD=seu-client-id-prod.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET_PROD=seu-client-secret-prod
```
*(Variáveis como `DATABASE_URL`, `JDBC_DATABASE_URL`, `REDIS_URL` e `PORT` são injetadas automaticamente pelos add-ons e dynos do Heroku).*
