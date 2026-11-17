package com.seniorinsight.backendcore.mensageria;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Comando durável de ativação de trial, entregue pelo menos uma vez ao billing. */
@Component
public class TrialOutbox {
    private static final Logger log = LoggerFactory.getLogger(TrialOutbox.class);
    private static final String TIPO = "billing.trial.requested";
    private final OutboxEventoRepository repositorio;
    private final ObjectMapper json;
    private final String tokenUrl, clientId, clientSecret, billingUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    TrialOutbox(OutboxEventoRepository repositorio, ObjectMapper json,
            @Value("${care.billing.token-url:}") String tokenUrl,
            @Value("${care.billing.client-id:}") String clientId,
            @Value("${care.billing.client-secret:}") String clientSecret,
            @Value("${care.billing.base-url:}") String billingUrl) {
        this.repositorio = repositorio;
        this.json = json;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.billingUrl = billingUrl;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(UUID familiarId, String sub) {
        try {
            repositorio.save(new OutboxEvento(UUID.randomUUID(), "familiar", familiarId, TIPO,
                    json.writeValueAsString(Map.of("sub", sub)), Instant.now()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Falha ao registrar trial na outbox", e);
        }
    }

    @Scheduled(fixedDelayString = "${care.billing.retry-interval:PT30S}")
    @Transactional
    public void entregarPendentes() {
        for (OutboxEvento evento : repositorio.travarTrials(20)) {
            try {
                entregar(json.readTree(evento.getPayload()).required("sub").asText());
                evento.marcarPublicado(Instant.now());
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                evento.registrarFalha();
                log.warn("Falha ao iniciar trial, evento {} permanece na outbox, tentativa {}", evento.getId(), evento.getTentativas());
                break;
            }
        }
    }

    private void entregar(String sub) throws Exception {
        if (tokenUrl.isBlank() || clientId.isBlank() || clientSecret.isBlank() || billingUrl.isBlank())
            throw new IllegalStateException("Credenciais do billing não configuradas");
        String form = "grant_type=client_credentials&client_id=" + codificar(clientId)
                + "&client_secret=" + codificar(clientSecret) + "&scope=billing.trial.write";
        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(tokenUrl)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> tokenResponse = http.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) throw new IllegalStateException("Token indisponível");
        String token = json.readTree(tokenResponse.body()).path("access_token").asText();
        if (token.isBlank()) throw new IllegalStateException("Token ausente");
        String base = billingUrl.endsWith("/") ? billingUrl.substring(0, billingUrl.length() - 1) : billingUrl;
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/internal/familias/" + codificar(sub) + "/trial"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> resposta = http.send(request, HttpResponse.BodyHandlers.discarding());
        if (resposta.statusCode() != 200) throw new IllegalStateException("Billing recusou solicitação");
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
