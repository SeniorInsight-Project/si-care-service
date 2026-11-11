-- ==============================================================================
-- Migração V12: contatos de emergência do idoso (HU-011)
--   - contatos_emergencia: até 3 contatos por idoso (regra aplicada no serviço), em ordem de prioridade
--   - telefone gravado em E.164 (ex.: +5511987654321)
-- ==============================================================================

CREATE TABLE contatos_emergencia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    nome VARCHAR(150) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    grau_parentesco VARCHAR(50),
    prioridade INTEGER NOT NULL CHECK (prioridade >= 1),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contatos_emergencia_idoso ON contatos_emergencia(idoso_id, prioridade);
