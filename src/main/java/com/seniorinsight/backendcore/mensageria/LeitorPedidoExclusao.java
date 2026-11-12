package com.seniorinsight.backendcore.mensageria;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.seniorinsight.backendcore.domain.PedidoExclusao;
import com.seniorinsight.backendcore.domain.TipoTitular;

/**
 * Valida o envelope de {@code lgpd.erasure.requested} (si-contracts/events) e o converte em {@link PedidoExclusao}.
 * Envelope fora do contrato lança {@link AmqpRejectAndDontRequeueException} e a mensagem vai para a {@code care.dlq}.
 */
@Component
class LeitorPedidoExclusao {

    static final String TIPO = "lgpd.erasure.requested";

    private static final String SPEC_VERSION = "1.0";
    private static final String SOURCE = "/report-service";
    private static final String CONTENT_TYPE = "application/json";
    private static final String DATA_SCHEMA = PublicadorEventos.URL_SCHEMAS + TIPO + ".v1.json";
    private static final int MAX_SUB = 255;

    PedidoExclusao ler(JsonNode envelope, String mensagemId) {
        exigir(SPEC_VERSION.equals(texto(envelope, "specversion")), "specversion diferente de " + SPEC_VERSION);
        exigir(SOURCE.equals(texto(envelope, "source")), "source diferente de " + SOURCE);
        exigir(TIPO.equals(texto(envelope, "type")), "type diferente de " + TIPO);
        exigir(DATA_SCHEMA.equals(texto(envelope, "dataschema")), "dataschema fora do contrato");
        exigir(CONTENT_TYPE.equals(texto(envelope, "datacontenttype")), "datacontenttype diferente de " + CONTENT_TYPE);
        exigir(uuid(envelope, "id").toString().equalsIgnoreCase(mensagemId), "id diferente do message_id");
        exigirDataHora(envelope);

        JsonNode data = envelope.path("data");
        exigir(data.isObject(), "sem data");
        UUID protocolo = uuid(data, "protocolo");
        exigir(protocolo.equals(uuid(envelope, "subject")), "subject diferente do protocolo");
        return new PedidoExclusao(protocolo, tipoTitular(data), uuid(data, "titularId"), titularSub(data));
    }

    private static void exigirDataHora(JsonNode envelope) {
        try {
            OffsetDateTime.parse(String.valueOf(texto(envelope, "time")));
        } catch (DateTimeParseException e) {
            throw invalido("time não é uma data e hora");
        }
    }

    private static TipoTitular tipoTitular(JsonNode data) {
        try {
            return TipoTitular.valueOf(String.valueOf(texto(data, "tipoTitular")));
        } catch (IllegalArgumentException e) {
            throw invalido("tipoTitular inválido");
        }
    }

    private static String titularSub(JsonNode data) {
        String sub = texto(data, "titularSub");
        exigir(sub != null && !sub.isBlank() && sub.length() <= MAX_SUB, "titularSub ausente ou inválido");
        return sub;
    }

    private static UUID uuid(JsonNode no, String campo) {
        String valor = texto(no, campo);
        try {
            UUID uuid = UUID.fromString(String.valueOf(valor));
            exigir(uuid.toString().equalsIgnoreCase(valor), campo + " não é um UUID");
            return uuid;
        } catch (IllegalArgumentException e) {
            throw invalido(campo + " não é um UUID");
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.get(campo);
        return valor != null && valor.isTextual() ? valor.asText() : null;
    }

    private static void exigir(boolean condicao, String motivo) {
        if (!condicao) {
            throw invalido(motivo);
        }
    }

    private static AmqpRejectAndDontRequeueException invalido(String motivo) {
        return new AmqpRejectAndDontRequeueException("Evento inválido: " + TIPO + " " + motivo);
    }
}
