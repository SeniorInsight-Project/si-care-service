package com.seniorinsight.backendcore.mensageria;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.service.ProvisionarFamiliarService;

/**
 * Consome a {@code care.q} (envelope CloudEvents, ver si-contracts/events). Trata {@code user.created}: perfil
 * FAMILIAR vira um Familiar; perfil IDOSO não é criado por aqui (o idoso entra pelo cadastro do familiar). Evento
 * inválido lança {@link AmqpRejectAndDontRequeueException} e, depois das tentativas, vai para a {@code care.dlq}.
 */
@Component
class ConsumidorCare extends ConsumidorIdempotente {

    static final String USER_CREATED = "user.created";

    private static final Logger log = LoggerFactory.getLogger(ConsumidorCare.class);
    private static final int MAX_SUB = 64;
    private static final int MAX_NOME = 150;
    private static final int MAX_EMAIL = 320;

    private final ObjectMapper objectMapper;
    private final ProvisionarFamiliarService provisionarFamiliar;

    ConsumidorCare(RegistroEventosProcessados registro, TransactionTemplate transacao, ObjectMapper objectMapper,
                   ProvisionarFamiliarService provisionarFamiliar) {
        super(registro, transacao);
        this.objectMapper = objectMapper;
        this.provisionarFamiliar = provisionarFamiliar;
    }

    @RabbitListener(queues = MensageriaConfig.FILA)
    void receber(Message mensagem) {
        processar(mensagem);
    }

    @Override
    protected String nome() {
        return "care";
    }

    @Override
    protected void tratar(String mensagemId, Message mensagem) {
        JsonNode envelope = lerEnvelope(mensagem);
        String tipo = texto(envelope, "type");
        if (!USER_CREATED.equals(tipo)) {
            log.info("Evento {} do tipo {} sem tratamento no care, ignorado", mensagemId, tipo);
            return;
        }
        tratarUserCreated(mensagemId, envelope.path("data"));
    }

    private void tratarUserCreated(String mensagemId, JsonNode data) {
        if (!data.isObject()) {
            throw invalido("user.created sem data");
        }
        String tipoPerfil = texto(data, "tipo");
        if ("IDOSO".equals(tipoPerfil)) {
            log.info("user.created {} é de um idoso, nada a criar no care", mensagemId);
            return;
        }
        if (!"FAMILIAR".equals(tipoPerfil)) {
            throw invalido("user.created com tipo inválido: " + tipoPerfil);
        }
        String sub = obrigatorio(data, "keycloakSub", MAX_SUB);
        String nome = obrigatorio(data, "nome", MAX_NOME);
        String email = texto(data, "email");
        if (email != null && (email.isBlank() || email.length() > MAX_EMAIL)) {
            throw invalido("user.created com email inválido");
        }
        provisionarFamiliar.criarSeAusente(sub, nome, email);
    }

    private JsonNode lerEnvelope(Message mensagem) {
        try {
            JsonNode envelope = objectMapper.readTree(mensagem.getBody());
            if (envelope == null || !envelope.isObject()) {
                throw invalido("corpo não é um envelope CloudEvents");
            }
            return envelope;
        } catch (IOException e) {
            throw invalido("corpo não é JSON: " + e.getMessage());
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.get(campo);
        return valor == null || valor.isNull() || !valor.isTextual() ? null : valor.asText();
    }

    private static String obrigatorio(JsonNode no, String campo, int maximo) {
        String valor = texto(no, campo);
        if (valor == null || valor.isBlank() || valor.length() > maximo) {
            throw invalido("user.created com " + campo + " ausente ou inválido");
        }
        return valor;
    }

    private static AmqpRejectAndDontRequeueException invalido(String motivo) {
        return new AmqpRejectAndDontRequeueException("Evento inválido: " + motivo);
    }
}
