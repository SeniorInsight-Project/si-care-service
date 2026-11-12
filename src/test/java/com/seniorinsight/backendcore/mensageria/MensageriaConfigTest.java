package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;

/** As routing keys da care.q vêm da configuração, não do código. */
class MensageriaConfigTest {

    private final MensageriaConfig config = new MensageriaConfig();

    @Test
    void cadaEventoConfiguradoViraUmBindingDaFilaCare() {
        List<String> routingKeys = bindingsDa(MensageriaConfig.EXCHANGE_EVENTOS,
                List.of("user.created", "lgpd.erasure.requested"));

        assertThat(routingKeys).containsExactlyInAnyOrder("user.created", "lgpd.erasure.requested");
    }

    @Test
    void aDlqContinuaLigadaPelaExchangeDeDeadLetter() {
        assertThat(bindingsDa(MensageriaConfig.EXCHANGE_DLX, List.of("user.created")))
                .containsExactly(MensageriaConfig.FILA);
    }

    private List<String> bindingsDa(String exchange, List<String> eventos) {
        Declarables declarables = config.bindings(config.exchangeEventos(), config.exchangeDeadLetter(),
                config.filaCare(), config.filaCareDlq(), eventos);
        return declarables.getDeclarables().stream()
                .filter(Binding.class::isInstance).map(Binding.class::cast)
                .filter(binding -> exchange.equals(binding.getExchange()))
                .map(Binding::getRoutingKey).toList();
    }
}
