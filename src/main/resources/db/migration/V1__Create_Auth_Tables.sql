-- ==============================================================================
-- Migração V1: Criação das Tabelas de Autenticação e Segurança (Épico 1)
-- Suporte às HUs:
--   - HU-001: SSO (Google/Apple)
--   - HU-002: Login Tradicional (e-mail + senha com hash)
--   - HU-003: Recuperação de Senha (token expirável)
--   - HU-004: MFA (Autenticação de 2 Fatores)
-- ==============================================================================

-- 1. Habilitação da extensão para suporte à geração de UUIDs v4
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Tabela de Usuários (HU-001, HU-002, HU-004)
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255), -- Nulo quando o usuário se autentica exclusivamente via SSO
    status VARCHAR(50) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'PENDENTE_MFA', 'BLOQUEADO')),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_segredo VARCHAR(255), -- Chave secreta TOTP (Base32) para verificação de 2 fatores
    provedor_sso VARCHAR(20) NOT NULL DEFAULT 'LOCAL' CHECK (provedor_sso IN ('LOCAL', 'GOOGLE', 'APPLE')),
    sso_id VARCHAR(255), -- Identificador único do usuário no provedor externo (Google sub / Apple sub)
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Tokens de Recuperação de Senha (HU-003)
CREATE TABLE tokens_recuperacao (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    data_expiracao TIMESTAMP WITH TIME ZONE NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Índices para Otimização de Consultas Críticas
-- Otimiza busca rápida no login tradicional e validação de existência de email (HU-002, HU-003)
CREATE INDEX idx_usuarios_email ON usuarios(email);

-- Otimiza consulta no fluxo de callback de login via SSO Google/Apple (HU-001)
CREATE INDEX idx_usuarios_sso ON usuarios(provedor_sso, sso_id);

-- Otimiza validação de tokens na confirmação de redefinição de senha (HU-003)
CREATE INDEX idx_tokens_recuperacao_token ON tokens_recuperacao(token);

-- Otimiza busca de histórico de tokens associados a um usuário específico (HU-003)
CREATE INDEX idx_tokens_recuperacao_usuario_id ON tokens_recuperacao(usuario_id);
