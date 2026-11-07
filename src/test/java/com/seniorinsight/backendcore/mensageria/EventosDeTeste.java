package com.seniorinsight.backendcore.mensageria;

import java.util.UUID;

/** Envelopes de teste no formato de si-contracts/events/user.created.v1.json. */
final class EventosDeTeste {

    private EventosDeTeste() {
    }

    static String userCreated(String sub, String tipo, String nome) {
        return """
                {"specversion":"1.0","id":"%s","source":"/identity-service","type":"user.created",
                 "time":"2026-10-31T12:00:00Z","datacontenttype":"application/json",
                 "dataschema":"https://raw.githubusercontent.com/SeniorInsight-Project/si-contracts/main/events/user.created.v1.json",
                 "subject":"%s",
                 "data":{"perfilId":"%s","keycloakSub":"%s","tipo":"%s","nome":"%s","email":"%s@exemplo.com",
                         "criadoEm":"2026-10-31T12:00:00Z"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), sub, tipo, nome, sub);
    }
}
