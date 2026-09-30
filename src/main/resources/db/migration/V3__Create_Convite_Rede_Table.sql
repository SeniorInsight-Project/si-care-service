-- ==============================================================================
-- Migração V3: Criação da Tabela de Convites da Rede de Proteção (HU16)
-- Suporte ao fluxo de convites por token temporal para vinculação de familiares a idosos
-- ==============================================================================

CREATE TABLE convites_rede (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    papel_acesso_sugerido VARCHAR(50) NOT NULL,
    grau_parentesco_sugerido VARCHAR(50) DEFAULT 'Familiar',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_expiracao TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_por_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização de consultas por token, idoso, email e status
CREATE INDEX idx_convites_rede_token ON convites_rede(token);
CREATE INDEX idx_convites_rede_idoso ON convites_rede(idoso_id);
CREATE INDEX idx_convites_rede_email ON convites_rede(email);
CREATE INDEX idx_convites_rede_status ON convites_rede(status);
