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
- **Branch Principal (`main` / `master`):** Produção/Homologação estável. É estritamente proibido fazer *push* direto para esta branch.
- **Branch de Desenvolvimento (`dev`):** Integração contínua das features em andamento.
- **Feature Branches:** Devem ser criadas a partir de `dev` seguindo o padrão `feature/nome-da-tarefa` ou `bugfix/descricao-do-erro`.
- **Pull Requests (PR):** Todo código inserido em `dev` ou `main` exige obrigatoriamente um Pull Request aprovado por pelo menos um membro listado no arquivo `CODEOWNERS` e aprovação automatizada das pipelines de CI/CD.

## 3. Segurança e LGPD (Compliance de Dados)
- **Zero Hardcoded Secrets:** É proibido armazenar senhas, tokens de API, chaves privadas SSH ou credenciais de banco de dados diretamente no código-fonte. Utilize sempre variáveis de ambiente (`.env`).
- **Criptografia:** Dados sensíveis em repouso devem respeitar padrões rigorosos de hash (ex: PBKDF2 com SHA-256) e tráfego estritamente sob TLS 1.3.
