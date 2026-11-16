-- ==============================================================================
-- Migração V13: exclusão dos dados do titular (HU-022, lgpd.erasure.requested)
--   - exclusao_protocolo: protocolos já tratados; a reentrega do mesmo pedido não refaz nem confirma de novo
--   - titular_excluido: hash SHA-256 (hex) do sub e do id dos titulares excluídos, para recusar a recriação
--     por um user.created antigo sem guardar o identificador
--   - aviso_exclusao_rede e aviso_exclusao_destinatario: cópia de quem da rede deve ser avisado quando os dados
--     de um idoso são excluídos; o notification-service consulta por protocolo e um job apaga depois do prazo
-- ==============================================================================

CREATE TABLE exclusao_protocolo (
    protocolo    UUID        NOT NULL,
    reservado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_exclusao_protocolo PRIMARY KEY (protocolo)
);

CREATE TABLE titular_excluido (
    impressao   VARCHAR(64) NOT NULL,
    excluido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_titular_excluido PRIMARY KEY (impressao)
);

CREATE TABLE aviso_exclusao_rede (
    protocolo           UUID         NOT NULL,
    primeiro_nome_idoso VARCHAR(150),
    criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_aviso_exclusao_rede PRIMARY KEY (protocolo)
);

CREATE INDEX ix_aviso_exclusao_rede_criado_em ON aviso_exclusao_rede (criado_em);

CREATE TABLE aviso_exclusao_destinatario (
    protocolo     UUID         NOT NULL REFERENCES aviso_exclusao_rede (protocolo) ON DELETE CASCADE,
    usuario_id    VARCHAR(64)  NOT NULL,
    nome_completo VARCHAR(255) NOT NULL,
    email         VARCHAR(320)
);

CREATE INDEX ix_aviso_exclusao_destinatario_protocolo ON aviso_exclusao_destinatario (protocolo);
