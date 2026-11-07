-- ==============================================================================
-- Migração V8: mensageria (RabbitMQ)
-- outbox_evento: eventos gravados na transação do caso de uso, publicados depois
--   na exchange si.events pelo publicador agendado (padrão outbox).
-- evento_processado: ids das mensagens já tratadas pelo consumidor, para que a
--   entrega "pelo menos uma vez" não repita o efeito (consumidor idempotente).
-- ==============================================================================

CREATE TABLE outbox_evento (
    id            UUID         NOT NULL DEFAULT uuid_generate_v4(),
    tipo_agregado VARCHAR(100) NOT NULL,
    agregado_id   UUID         NOT NULL,
    tipo_evento   VARCHAR(150) NOT NULL,
    payload       JSONB        NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    publicado_em  TIMESTAMPTZ,
    tentativas    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_outbox_evento PRIMARY KEY (id)
);

COMMENT ON COLUMN outbox_evento.tipo_evento IS 'Routing key na exchange si.events, ex.: sos.updated';

-- O publicador só lê pendentes, em ordem de criação.
CREATE INDEX ix_outbox_evento_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;

CREATE TABLE evento_processado (
    mensagem_id   VARCHAR(100) NOT NULL,
    consumidor    VARCHAR(100) NOT NULL,
    processado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_evento_processado PRIMARY KEY (mensagem_id, consumidor)
);
