package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class TrialOutboxTest {
    @Test
    void falhaMantemPendenteERepeticaoEntregaComToken() throws Exception {
        var repo = mock(OutboxEventoRepository.class);
        var evento = new OutboxEvento(UUID.randomUUID(), "familiar", UUID.randomUUID(),
                "billing.trial.requested", "{\"sub\":\"abc\"}", Instant.now());
        when(repo.travarTrials(20)).thenReturn(List.of(evento));
        var tentativas = new AtomicInteger();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", e -> {
            String form = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(form).contains("grant_type=client_credentials", "scope=billing.trial.write");
            byte[] corpo = "{\"access_token\":\"teste\"}".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, corpo.length); e.getResponseBody().write(corpo); e.close();
        });
        servidor.createContext("/internal/familias/abc/trial", e -> {
            assertThat(e.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer teste");
            e.sendResponseHeaders(tentativas.incrementAndGet() == 1 ? 503 : 200, -1); e.close();
        });
        servidor.start();
        try {
            String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
            var outbox = new TrialOutbox(repo, new ObjectMapper(), url + "/token", "care-service", "secret", url);
            outbox.entregarPendentes();
            assertThat(evento.getPublicadoEm()).isNull();
            assertThat(evento.getTentativas()).isEqualTo(1);
            outbox.entregarPendentes();
            assertThat(evento.getPublicadoEm()).isNotNull();
            assertThat(tentativas.get()).isEqualTo(2);
        } finally { servidor.stop(0); }
    }
}
