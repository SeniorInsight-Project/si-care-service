package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por simular o envio de e-mails da plataforma SeniorInsight (HU16).
 * Registra logs detalhados e estruturados do convite disparado para o destinatário.
 */
@Service
public class SimuladorEmailService {

    private static final Logger log = LoggerFactory.getLogger(SimuladorEmailService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Simula o envio de e-mail de convite para a Rede de Proteção.
     *
     * @param emailDestinatario E-mail do familiar convidado
     * @param nomeIdoso Nome completo do idoso monitorado
     * @param token Token temporal gerado via UUID
     * @param papelAcesso Papel de acesso sugerido
     * @param dataExpiracao Data limite de validade de 48h
     * @param mensagemPersonalizada Mensagem opcional do remetente
     */
    public void simularEnvioConvite(String emailDestinatario, String nomeIdoso, String token,
                                   String papelAcesso, Instant dataExpiracao, String mensagemPersonalizada) {
        String dataFormatada = FORMATTER.format(dataExpiracao);

        log.info("""
                
                ================================================================================
                [SENIORINSIGHT - SIMULAÇÃO DE DISPARO DE E-MAIL]
                ================================================================================
                Para: %s
                Assunto: Convite para integrar a Rede de Proteção de %s
                --------------------------------------------------------------------------------
                Olá!
                
                Você foi convidado para integrar a Rede de Proteção e Cuidado de %s com o papel:
                [%s].
                %s
                Para aceitar o convite e associar sua conta de Familiar ao idoso, utilize o token:
                --------------------------------------------------------------------------------
                TOKEN TEMPORAL: %s
                --------------------------------------------------------------------------------
                ATENÇÃO: Este token possui validade de 48 horas (expira em: %s).
                ================================================================================
                """.formatted(
                emailDestinatario,
                nomeIdoso,
                nomeIdoso,
                papelAcesso,
                mensagemPersonalizada != null && !mensagemPersonalizada.isBlank()
                        ? "\nMensagem do remetente:\n\"" + mensagemPersonalizada.trim() + "\"\n"
                        : "",
                token,
                dataFormatada
        ));
    }
}
