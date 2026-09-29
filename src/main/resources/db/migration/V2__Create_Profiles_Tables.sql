-- ==============================================================================
-- Migração V2: Criação das Tabelas de Perfis e Entidades Físicas (HU-002 / DRS)
-- Suporte aos domínios:
--   - Familiar: Perfil completo do familiar associado à conta de usuário
--   - Medico: Perfil completo do médico associado à conta de usuário
--   - Idoso: Dados físicos, clínicos e cadastrais do idoso monitorado
--   - VinculoRede: Relação entre Familiar e Idoso com papéis e restrições JSONB
--   - MedicosIdosos: Tabela associativa entre Médicos e Idosos monitorados
-- ==============================================================================

-- 1. Tabela de Familiares
CREATE TABLE familiares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    nome_completo VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    cpf VARCHAR(14) NOT NULL UNIQUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Médicos
CREATE TABLE medicos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    nome_completo VARCHAR(255) NOT NULL,
    crm VARCHAR(50) NOT NULL UNIQUE,
    especialidade VARCHAR(100) NOT NULL,
    telefone_consultorio VARCHAR(20),
    nome_clinica VARCHAR(255),
    cnpj VARCHAR(20),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Idosos
CREATE TABLE idosos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome_completo VARCHAR(255) NOT NULL,
    data_nascimento DATE NOT NULL,
    genero VARCHAR(20),
    cpf VARCHAR(14) UNIQUE,
    foto_perfil_url VARCHAR(500),
    tipo_sanguineo VARCHAR(5),
    comorbidades JSONB DEFAULT '[]'::jsonb,
    alergias TEXT,
    limites_basais JSONB DEFAULT '{}'::jsonb,
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(50),
    cep VARCHAR(20),
    plano_saude VARCHAR(100),
    observacoes_socorros TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela de Vínculos de Rede (Familiar <-> Idoso)
CREATE TABLE vinculos_rede (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    familiar_id UUID NOT NULL REFERENCES familiares(id) ON DELETE CASCADE,
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    grau_parentesco VARCHAR(50) NOT NULL,
    papel_acesso VARCHAR(50) NOT NULL,
    restricoes_acesso JSONB DEFAULT '{}'::jsonb,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vinculos_rede_familiar_idoso UNIQUE (familiar_id, idoso_id)
);

-- 5. Tabela Associativa Médicos <-> Idosos
CREATE TABLE medicos_idosos (
    medico_id UUID NOT NULL REFERENCES medicos(id) ON DELETE CASCADE,
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (medico_id, idoso_id)
);

-- 6. Índices para Otimização de Consultas Críticas
CREATE INDEX idx_familiares_usuario_id ON familiares(usuario_id);
CREATE INDEX idx_familiares_cpf ON familiares(cpf);

CREATE INDEX idx_medicos_usuario_id ON medicos(usuario_id);
CREATE INDEX idx_medicos_crm ON medicos(crm);

CREATE INDEX idx_idosos_cpf ON idosos(cpf);
CREATE INDEX idx_idosos_nome ON idosos(nome_completo);

CREATE INDEX idx_vinculos_rede_familiar ON vinculos_rede(familiar_id);
CREATE INDEX idx_vinculos_rede_idoso ON vinculos_rede(idoso_id);

CREATE INDEX idx_medicos_idosos_idoso ON medicos_idosos(idoso_id);
