package com.seniorinsight.backendcore.listener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.event.SosAcionadoEvent;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

/**
 * Listener de Eventos Assíncrono para Alertas de Emergência / Botão SOS (HU35).
 * Recebe o evento `SosAcionadoEvent` desacoplado da requisição HTTP principal,
 * cumprindo o SLA crítico (< 100ms) ao processar as notificações de emergência
 * para toda a rede de proteção em background na thread dedicada `sosTaskExecutor`.
 */
@Component
public class SosEventListener {

    private static final Logger log = LoggerFactory.getLogger(SosEventListener.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final VinculoRedeRepository vinculoRedeRepository;
    private final IdosoRepository idosoRepository;

    public SosEventListener(VinculoRedeRepository vinculoRedeRepository, IdosoRepository idosoRepository) {
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.idosoRepository = idosoRepository;
    }

    /**
     * Processa assincronamente as notificações para a rede de proteção do idoso após a confirmação da transação.
     *
     * @param event Dados do alarme SOS disparado
     */
    @Async("sosTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void processarNotificacoesSos(SosAcionadoEvent event) {
        log.warn("=== [SISTEMA SOS - HU35] DISPARO DE EMERGÊNCIA DETECTADO ===");
        log.warn("Alarme ID: {} | Idoso: {} | Bateria: {}% | Horário: {}",
                event.alarmeId(), event.idosoId(),
                event.nivelBateria() != null ? event.nivelBateria() : "N/D",
                event.acionadoEm() != null ? FORMATTER.format(event.acionadoEm()) : "Agora");

        String mapsUrl = (event.latitude() != null && event.longitude() != null)
                ? "https://www.google.com/maps?q=" + event.latitude() + "," + event.longitude()
                : "Coordenadas GPS não informadas ou indisponíveis";

        // Obtém dados complementares do idoso para enriquecimento do socorro emergencial
        Idoso idoso = idosoRepository.findById(event.idosoId()).orElse(null);
        String tipoSanguineo = (idoso != null && idoso.getTipoSanguineo() != null) ? idoso.getTipoSanguineo() : "Não informado";
        String alergias = (idoso != null && idoso.getAlergias() != null) ? idoso.getAlergias() : "Nenhuma relatada";
        String observacoesSocorro = (idoso != null && idoso.getObservacoesSocorros() != null) ? idoso.getObservacoesSocorros() : "Sem observações prévias";

        // Busca todos os familiares e cuidadores vinculados à rede de proteção
        List<VinculoRede> redeProtecao = vinculoRedeRepository.findByIdosoIdWithFamiliar(event.idosoId());

        if (redeProtecao.isEmpty()) {
            log.error("[SISTEMA SOS] ATENÇÃO: Idoso {} não possui membros vinculados na Rede de Proteção!", event.idosoId());
        } else {
            log.info("[SISTEMA SOS] Disparando notificações multicanal para {} membro(s) da rede de proteção.",
                    redeProtecao.size());

            for (VinculoRede vinculo : redeProtecao) {
                enviarNotificacaoEmergencia(vinculo, event, mapsUrl, tipoSanguineo, alergias, observacoesSocorro);
            }
        }

        log.info("[SISTEMA SOS] Processamento assíncrono concluído para o alarme ID: {}", event.alarmeId());
    }

    private void enviarNotificacaoEmergencia(VinculoRede vinculo, SosAcionadoEvent event, String mapsUrl,
                                             String tipoSanguineo, String alergias, String observacoesSocorro) {
        log.warn("[SISTEMA SOS] Alerta do alarme {} enviado (simulado) ao vínculo {} ({}).",
                event.alarmeId(), vinculo.getId(), vinculo.getPapelAcesso());
        if (!log.isDebugEnabled()) {
            return;
        }
        // Mensagem completa só em DEBUG (ligado no perfil dev): tem nome, e-mail, localização e dados de saúde.
        String email = vinculo.getFamiliar().getUsuario().getEmail();
        String nomeFamiliar = vinculo.getFamiliar().getNomeCompleto();
        String grauParentesco = vinculo.getGrauParentesco();
        String papelAcesso = vinculo.getPapelAcesso();

        log.debug("""
                
                ================================================================================
                [SENIORINSIGHT - ALERTA CRÍTICO DE EMERGÊNCIA / SOS DISPARADO]
                ================================================================================
                DESTINATÁRIO: %s (%s - %s)
                E-MAIL: %s
                --------------------------------------------------------------------------------
                ATENÇÃO IMEDIATA:
                O idoso %s acionou o BOTÃO DE ALARME / SOS DE EMERGÊNCIA!
                
                DETALHES DO EVENTO:
                - Alarme ID: %s
                - Horário do Acionamento: %s
                - Nível de Bateria do Dispositivo: %s%%
                - Localização no Mapa: %s
                - Observações do Disparo: %s
                
                DADOS MÉDICOS EMERGENCIAIS:
                - Tipo Sanguíneo: %s
                - Alergias Registradas: %s
                - Recomendações de Socorro: %s
                --------------------------------------------------------------------------------
                AÇÃO RECOMENDADA: Entre em contato imediatamente com o idoso ou dirija-se ao local.
                Em caso de não resposta, contate os serviços médicos de emergência (SAMU 192).
                ================================================================================
                """.formatted(
                nomeFamiliar,
                grauParentesco,
                papelAcesso,
                email,
                event.nomeIdoso(),
                event.alarmeId(),
                event.acionadoEm() != null ? FORMATTER.format(event.acionadoEm()) : "Agora",
                event.nivelBateria() != null ? event.nivelBateria().toString() : "N/D",
                mapsUrl,
                event.observacao() != null && !event.observacao().isBlank() ? event.observacao() : "Nenhuma",
                tipoSanguineo,
                alergias,
                observacoesSocorro
        ));
    }
}
