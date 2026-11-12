package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * lgpd.erasure.requested de ponta a ponta, com PostgreSQL e RabbitMQ reais e as migrações do Flyway: o idoso e tudo
 * que depende dele somem, o familiar some e o idoso fica, o aviso à rede é gravado e a confirmação sai uma vez.
 */
class ExclusaoTitularIntegracaoTest extends IntegracaoBase {

    private static final Duration ESPERA = Duration.ofSeconds(20);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exclusaoDoIdosoApagaTudoGravaOAvisoDaRedeEConfirmaUmaVez() throws Exception {
        UUID idoso = idoso("Dona Sebastiana Lima");
        UUID admin = familiar("sub-admin", "Ana Lima", "ana@exemplo.com");
        UUID apoio = familiar("sub-apoio", "Caio Lima", null);
        vincular(admin, idoso, "ADMINISTRADOR");
        vincular(apoio, idoso, "APOIO");
        popularDependentesDoIdoso(idoso, admin);
        assertThat(tabelasComIdosoSemLinhas(idoso)).as("seed cobre toda tabela com idoso_id").isEmpty();
        UUID protocolo = UUID.randomUUID();

        enviar(UUID.randomUUID(), protocolo, "IDOSO", idoso, "sub-do-idoso");

        aguardarConfirmacao(protocolo);
        assertThat(tabelasComIdosoComLinhas(idoso)).as("tabelas que ainda têm dados do idoso").isEmpty();
        assertThat(contar("SELECT count(*) FROM idosos WHERE id = ?", idoso)).isZero();
        assertThat(contar("SELECT count(*) FROM familiares WHERE id IN (?, ?)", admin, apoio)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT primeiro_nome_idoso FROM aviso_exclusao_rede WHERE protocolo = ?",
                String.class, protocolo)).isEqualTo("Dona");
        assertThat(jdbc.queryForList("""
                SELECT usuario_id || '|' || nome_completo || '|' || coalesce(email, '-')
                  FROM aviso_exclusao_destinatario WHERE protocolo = ? ORDER BY usuario_id
                """, String.class, protocolo)).containsExactly("sub-admin|Ana Lima|ana@exemplo.com",
                "sub-apoio|Caio Lima|-");
        assertThat(jdbc.queryForList("SELECT impressao FROM titular_excluido", String.class))
                .contains(sha256("sub-do-idoso"), sha256(idoso.toString()))
                .doesNotContain("sub-do-idoso", idoso.toString());
    }

    @Test
    void confirmacaoSegueOContratoEReentregaDoProtocoloNaoPublicaDeNovo() throws Exception {
        UUID protocolo = UUID.randomUUID();
        UUID titular = UUID.randomUUID();

        enviar(UUID.randomUUID(), protocolo, "IDOSO", titular, "sub-sem-care");
        enviar(UUID.randomUUID(), protocolo, "IDOSO", titular, "sub-sem-care");
        UUID sentinela = UUID.randomUUID();
        enviar(UUID.randomUUID(), sentinela, "IDOSO", UUID.randomUUID(), "sub-sentinela");

        aguardarConfirmacao(sentinela);
        assertThat(confirmacoes(protocolo)).hasSize(1);
        JsonNode envelope = objectMapper.readTree(confirmacoes(protocolo).get(0));
        assertThat(envelope.get("source").asText()).isEqualTo("/care-service");
        assertThat(envelope.get("type").asText()).isEqualTo("lgpd.erasure.completed");
        assertThat(envelope.get("subject").asText()).isEqualTo(protocolo.toString());
        assertThat(envelope.get("dataschema").asText()).isEqualTo("https://raw.githubusercontent.com/"
                + "SeniorInsight-Project/si-contracts/main/events/lgpd.erasure.completed.v1.json");
        assertThat(envelope.get("data").get("protocolo").asText()).isEqualTo(protocolo.toString());
        assertThat(envelope.get("data").get("servico").asText()).isEqualTo("care-service");
        assertThat(envelope.get("data")).hasSize(2);
    }

    @Test
    void idosoSemCadastroNoCareSoConfirmaEDeixaOAvisoVazio() throws Exception {
        UUID protocolo = UUID.randomUUID();

        enviar(UUID.randomUUID(), protocolo, "IDOSO", UUID.randomUUID(), "sub-sem-care");

        aguardarConfirmacao(protocolo);
        assertThat(contar("SELECT count(*) FROM aviso_exclusao_destinatario WHERE protocolo = ?", protocolo)).isZero();
        assertThat(contar("SELECT count(*) FROM aviso_exclusao_rede WHERE protocolo = ?", protocolo)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM titular_excluido WHERE impressao = ?", sha256("sub-sem-care")))
                .isZero();
    }

    @Test
    void exclusaoDoFamiliarApagaPeloSubMantemOIdosoERegistraQueFicouSemAdministrador() throws Exception {
        UUID idoso = idoso("Seu Jorge");
        UUID outroIdoso = idoso("Dona Maria");
        UUID titular = familiar("sub-titular", "Ana Lima", "ana@exemplo.com");
        UUID colega = familiar("sub-colega", "Caio Lima", "caio@exemplo.com");
        vincular(titular, idoso, "ADMINISTRADOR");
        vincular(titular, outroIdoso, "APOIO");
        vincular(colega, outroIdoso, "ADMINISTRADOR");
        jdbc.update("""
                INSERT INTO convites_rede (idoso_id, email, token, papel_acesso_sugerido, data_expiracao, criado_por_sub)
                VALUES (?, 'ana@exemplo.com', 'tk-convidada', 'APOIO', now() + interval '1 day', 'sub-colega'),
                       (?, 'novo@exemplo.com', 'tk-emitido', 'APOIO', now() + interval '1 day', 'sub-titular')
                """, outroIdoso, idoso);
        jdbc.update("""
                INSERT INTO consentimentos (idoso_id, tipo, destinatario, autorizado, versao_termo, autor_sub)
                VALUES (?, 'PASSOS', 'REDE', true, 'v1', 'sub-titular')
                """, idoso);
        UUID protocolo = UUID.randomUUID();

        enviar(UUID.randomUUID(), protocolo, "FAMILIAR", UUID.randomUUID(), "sub-titular");

        aguardarConfirmacao(protocolo);
        assertThat(contar("SELECT count(*) FROM familiares WHERE keycloak_sub = 'sub-titular'")).isZero();
        assertThat(contar("SELECT count(*) FROM vinculos_rede WHERE familiar_id = ?", titular)).isZero();
        assertThat(contar("SELECT count(*) FROM idosos WHERE id IN (?, ?)", idoso, outroIdoso)).isEqualTo(2);
        assertThat(contar("SELECT count(*) FROM familiares WHERE id = ?", colega)).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM convites_rede WHERE token = 'tk-convidada'")).isZero();
        assertThat(jdbc.queryForObject("SELECT criado_por_sub FROM convites_rede WHERE token = 'tk-emitido'",
                String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT autor_sub FROM consentimentos WHERE idoso_id = ?", String.class, idoso))
                .isEqualTo("titular-excluido");
        assertThat(jdbc.queryForList("""
                SELECT idoso_id::text FROM auditoria_idosos
                 WHERE valores_novos->>'administrador' = 'false' AND valores_novos->>'protocolo' = ?
                """, String.class, protocolo.toString())).containsExactly(idoso.toString());
        assertThat(contar("SELECT count(*) FROM titular_excluido WHERE impressao = ?", sha256("sub-titular")))
                .isEqualTo(1);
    }

    @Test
    void userCreatedDepoisDaExclusaoNaoRecriaOFamiliar() throws Exception {
        UUID protocolo = UUID.randomUUID();
        String sub = "sub-" + UUID.randomUUID();
        familiar(sub, "Ana Lima", "ana@exemplo.com");
        enviar(UUID.randomUUID(), protocolo, "FAMILIAR", UUID.randomUUID(), sub);
        aguardarConfirmacao(protocolo);

        enviarUserCreated(sub);
        String sentinela = "sub-" + UUID.randomUUID();
        enviarUserCreated(sentinela);

        await().atMost(ESPERA).untilAsserted(
                () -> assertThat(contar("SELECT count(*) FROM familiares WHERE keycloak_sub = ?", sentinela)).isEqualTo(1));
        assertThat(contar("SELECT count(*) FROM familiares WHERE keycloak_sub = ?", sub)).isZero();
    }

    @Test
    void envelopeInvalidoVaiParaADlqENaoApagaNada() {
        UUID idoso = idoso("Seu Jorge");
        UUID protocolo = UUID.randomUUID();
        UUID mensagem = UUID.randomUUID();
        String corpo = EventosDeTeste.exclusaoSolicitada(mensagem, protocolo, "IDOSO", idoso, "sub")
                .replace("/report-service", "/identity-service");

        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "lgpd.erasure.requested", mensagem(mensagem, corpo));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(naDlq(protocolo)).isTrue());
        assertThat(contar("SELECT count(*) FROM idosos WHERE id = ?", idoso)).isEqualTo(1);
        assertThat(confirmacoes(protocolo)).isEmpty();
    }

    private boolean naDlq(UUID protocolo) {
        Message morta = rabbitTemplate.receive(MensageriaConfig.FILA_DLQ);
        return morta != null && new String(morta.getBody(), StandardCharsets.UTF_8).contains(protocolo.toString());
    }

    private void popularDependentesDoIdoso(UUID idoso, UUID familiar) {
        UUID medicamento = UUID.randomUUID();
        UUID rotina = UUID.randomUUID();
        UUID medico = UUID.randomUUID();
        jdbc.update("INSERT INTO medicamentos (id, nome, dosagem) VALUES (?, 'Losartana', '50mg')", medicamento);
        jdbc.update("""
                INSERT INTO rotinas_medicamentos (id, idoso_id, medicamento_id, horario, frequencia)
                VALUES (?, ?, ?, TIME '08:00', 'DIARIA')
                """, rotina, idoso, medicamento);
        jdbc.update("""
                INSERT INTO historicos_medicacao (rotina_medicamento_id, status, data_hora_ingestao, registrado_por_sub)
                VALUES (?, 'TOMADO', now(), 'sub-admin')
                """, rotina);
        jdbc.update("INSERT INTO alarmes_emergencia (idoso_id, status) VALUES (?, 'ABERTO')", idoso);
        jdbc.update("INSERT INTO telemetria_wearable (idoso_id, coletado_em) VALUES (?, now())", idoso);
        jdbc.update("INSERT INTO contatos_emergencia (idoso_id, nome, telefone, prioridade) VALUES (?, 'Ana', '11999990000', 1)",
                idoso);
        jdbc.update("""
                INSERT INTO consentimentos (idoso_id, tipo, destinatario, autorizado, versao_termo, autor_sub)
                VALUES (?, 'PASSOS', 'REDE', true, 'v1', 'sub-admin')
                """, idoso);
        jdbc.update("""
                INSERT INTO convites_rede (idoso_id, email, token, papel_acesso_sugerido, data_expiracao)
                VALUES (?, 'convidado@exemplo.com', ?, 'APOIO', now() + interval '1 day')
                """, idoso, "tk-" + idoso);
        jdbc.update("INSERT INTO curatelas_idoso (idoso_id, familiar_id) VALUES (?, ?)", idoso, familiar);
        jdbc.update("""
                INSERT INTO auditoria_idosos (idoso_id, autor_sub, valores_anteriores, valores_novos)
                VALUES (?, 'sub-admin', '{}'::jsonb, '{}'::jsonb)
                """, idoso);
        jdbc.update("INSERT INTO medicos (id, keycloak_sub, nome_completo, crm, especialidade) VALUES (?, ?, 'Dr.', ?, 'Geriatria')",
                medico, "sub-medico-" + medico, "CRM-" + medico);
        jdbc.update("INSERT INTO medicos_idosos (medico_id, idoso_id) VALUES (?, ?)", medico, idoso);
    }

    private List<String> tabelasComIdosoSemLinhas(UUID idoso) {
        return tabelasComIdoso().stream().filter(tabela -> contarNaTabela(tabela, idoso) == 0).toList();
    }

    private List<String> tabelasComIdosoComLinhas(UUID idoso) {
        return tabelasComIdoso().stream().filter(tabela -> contarNaTabela(tabela, idoso) > 0).toList();
    }

    private List<String> tabelasComIdoso() {
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND column_name = 'idoso_id' ORDER BY table_name
                """, String.class);
    }

    private int contarNaTabela(String tabela, UUID idoso) {
        return contar("SELECT count(*) FROM " + tabela + " WHERE idoso_id = ?", idoso);
    }

    private UUID idoso(String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO idosos (id, nome_completo, data_nascimento) VALUES (?, ?, DATE '1950-01-01')", id, nome);
        return id;
    }

    private UUID familiar(String sub, String nome, String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO familiares (id, keycloak_sub, nome_completo, email) VALUES (?, ?, ?, ?)",
                id, sub, nome, email);
        return id;
    }

    private void vincular(UUID familiar, UUID idoso, String papel) {
        jdbc.update("""
                INSERT INTO vinculos_rede (familiar_id, idoso_id, grau_parentesco, papel_acesso)
                VALUES (?, ?, 'FILHO', ?)
                """, familiar, idoso, papel);
    }

    private void enviar(UUID mensagem, UUID protocolo, String tipo, UUID titular, String sub) {
        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "lgpd.erasure.requested",
                mensagem(mensagem, EventosDeTeste.exclusaoSolicitada(mensagem, protocolo, tipo, titular, sub)));
    }

    private void enviarUserCreated(String sub) {
        rabbitTemplate.send(MensageriaConfig.EXCHANGE_EVENTOS, "user.created", mensagem(UUID.randomUUID(),
                EventosDeTeste.userCreated(sub, "FAMILIAR", "Ana Lima")));
    }

    private static Message mensagem(UUID id, String corpo) {
        return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(id.toString())
                .build();
    }

    private void aguardarConfirmacao(UUID protocolo) {
        await().atMost(ESPERA).untilAsserted(() -> assertThat(confirmacoes(protocolo)).isNotEmpty());
    }

    private List<String> confirmacoes(UUID protocolo) {
        return jdbc.queryForList("""
                SELECT payload::text FROM outbox_evento
                 WHERE tipo_evento = 'lgpd.erasure.completed' AND agregado_id = ?
                """, String.class, protocolo);
    }

    private String sha256(String valor) {
        return jdbc.queryForObject("SELECT encode(sha256(convert_to(?, 'UTF8')), 'hex')", String.class, valor);
    }

    private int contar(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }
}
