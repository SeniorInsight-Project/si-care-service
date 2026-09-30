-- ==============================================================================
-- Migração V5: Criação da Tabela de Alarmes de Emergência / Botão SOS (HU35)
-- Suporte ao registro em tempo real de disparos de socorro, geolocalização e status
-- ==============================================================================

CREATE TABLE alarmes_emergencia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    nivel_bateria INTEGER,
    status VARCHAR(30) NOT NULL,
    observacao TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para Otimização de Consultas Críticas e SLA
CREATE INDEX idx_alarmes_emergencia_idoso ON alarmes_emergencia(idoso_id);
CREATE INDEX idx_alarmes_emergencia_status ON alarmes_emergencia(status);
CREATE INDEX idx_alarmes_emergencia_criado_em ON alarmes_emergencia(criado_em DESC);
