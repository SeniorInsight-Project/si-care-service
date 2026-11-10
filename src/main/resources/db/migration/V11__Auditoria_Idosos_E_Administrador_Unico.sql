-- ==============================================================================
-- Migração V11: auditoria da edição do idoso (HU-009) e administrador único (RN-012)
--   - auditoria_idosos: quem alterou o cadastro, quando, e os valores antes e depois
--   - índice parcial: cada idoso tem no máximo um vínculo ADMINISTRADOR
-- ==============================================================================

CREATE TABLE auditoria_idosos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    autor_sub VARCHAR(64) NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valores_anteriores JSONB NOT NULL,
    valores_novos JSONB NOT NULL
);

CREATE INDEX idx_auditoria_idosos_idoso ON auditoria_idosos(idoso_id, ocorrido_em);

CREATE UNIQUE INDEX uq_vinculos_rede_administrador_por_idoso
    ON vinculos_rede(idoso_id) WHERE papel_acesso = 'ADMINISTRADOR';
