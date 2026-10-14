# 🛡️ Políticas de Governança e Contribuição - SeniorInsight

Este documento estabelece as diretrizes obrigatórias de desenvolvimento, segurança e conformidade para todos os contribuidores dos repositórios da organização.

## 1. Padrão de Versionamento e Commits (Conventional Commits)
Para garantir clareza e rastreabilidade no histórico do projeto, todas as mensagens de commit devem seguir estritamente a especificação **Conventional Commits**:
- `feat: ` Introdução de uma nova funcionalidade (Requisito Funcional).
- `fix: ` Correção de um bug ou anomalia sistêmica.
- `docs: ` Alterações exclusivas na documentação (ex: README, DRS).
- `style: ` Ajustes de formatação, ponto e vírgula, que não afetam a lógica do código.
- `refactor: ` Alteração de código que não corrige bugs nem adiciona recursos (refatoração).
- `test: ` Adição ou correção de testes unitários/integrados.
- `chore: ` Atualizações de tarefas de build, gerenciadores de pacotes ou configs de CI/CD.

## 2. Fluxo de Trabalho (Git Workflow)
- **Branch Principal (`main`):** Produção. Push direto bloqueado; só recebe Pull Request vindo de `develop` (ou de `hotfix/*` para correção urgente).
- **Branch de Desenvolvimento (`develop`):** Testes e integração das features; é a branch padrão do repositório. Push direto bloqueado.
- **Branches de card:** Criadas a partir de `develop` pelo botão *Create a branch* do card no quadro (ex.: `84-hu-063-back-disparo-do-sos-e-estados`).
- **Pull Requests (PR):** Todo código entra em `develop` ou `main` por Pull Request aprovado pelo responsável listado no `CODEOWNERS`. Passo a passo em `CONTRIBUTING.md`.

## 3. Segurança e LGPD (Compliance de Dados)
- **Zero Hardcoded Secrets:** É proibido armazenar senhas, tokens de API, chaves privadas SSH ou credenciais de banco de dados diretamente no código-fonte. Utilize sempre variáveis de ambiente (`.env`).
- **Criptografia:** Dados sensíveis em repouso devem respeitar padrões rigorosos de hash (ex: PBKDF2 com SHA-256) e tráfego estritamente sob TLS 1.3.
