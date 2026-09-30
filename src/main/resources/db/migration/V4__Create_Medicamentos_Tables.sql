-- ==============================================================================
-- Migração V4: Criação das Tabelas do Módulo de Medicamentos e Rotinas (HU26)
-- Suporte ao cadastro de medicamentos, rotinas de ingestão e histórico de adesão
-- ==============================================================================

-- 1. Tabela de Medicamentos (Catálogo / Prescrição)
CREATE TABLE medicamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    dosagem VARCHAR(100) NOT NULL,
    instrucoes TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Rotinas de Medicamentos (Associação Idoso <-> Medicamento com horários e frequência)
CREATE TABLE rotinas_medicamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    medicamento_id UUID NOT NULL REFERENCES medicamentos(id) ON DELETE CASCADE,
    horario TIME NOT NULL,
    frequencia VARCHAR(100) NOT NULL,
    dias_semana VARCHAR(100) DEFAULT 'TODOS',
    data_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    data_fim DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Histórico de Medicação (Log de ingestão com status TOMADO, ATRASADO, IGNORADO)
CREATE TABLE historicos_medicacao (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rotina_medicamento_id UUID NOT NULL REFERENCES rotinas_medicamentos(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    data_hora_ingestao TIMESTAMP WITH TIME ZONE NOT NULL,
    data_hora_prevista TIMESTAMP WITH TIME ZONE,
    observacao VARCHAR(500),
    registrado_por_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Índices para Otimização de Consultas Críticas
CREATE INDEX idx_medicamentos_nome ON medicamentos(nome);
CREATE INDEX idx_rotinas_medicamentos_idoso ON rotinas_medicamentos(idoso_id);
CREATE INDEX idx_rotinas_medicamentos_medicamento ON rotinas_medicamentos(medicamento_id);
CREATE INDEX idx_rotinas_medicamentos_horario ON rotinas_medicamentos(horario);
CREATE INDEX idx_rotinas_medicamentos_ativo ON rotinas_medicamentos(ativo);
CREATE INDEX idx_historicos_medicacao_rotina ON historicos_medicacao(rotina_medicamento_id);
CREATE INDEX idx_historicos_medicacao_status ON historicos_medicacao(status);
CREATE INDEX idx_historicos_medicacao_ingestao ON historicos_medicacao(data_hora_ingestao);
