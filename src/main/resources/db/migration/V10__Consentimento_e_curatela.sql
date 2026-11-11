CREATE TABLE curatelas_idoso (
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    familiar_id UUID NOT NULL REFERENCES familiares(id) ON DELETE CASCADE,
    registrada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    revogada_em TIMESTAMPTZ,
    PRIMARY KEY (idoso_id, familiar_id),
    CHECK (revogada_em IS NULL OR revogada_em >= registrada_em)
);

CREATE TABLE consentimentos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idoso_id UUID NOT NULL REFERENCES idosos(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('BATIMENTOS', 'PASSOS', 'SONO', 'OXIGENACAO', 'LOCALIZACAO')),
    destinatario VARCHAR(20) NOT NULL CHECK (destinatario IN ('REDE', 'MEDICO', 'OPERADORA')),
    autorizado BOOLEAN NOT NULL,
    versao_termo VARCHAR(50) NOT NULL,
    autor_sub VARCHAR(255) NOT NULL,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX ix_consentimentos_atual ON consentimentos (idoso_id, destinatario, tipo, id DESC);
