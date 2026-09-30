package com.seniorinsight.backendcore.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio imutável (Record) representando o acionamento de um Alarme SOS de Emergência (HU35).
 * Disparado via Spring Application Events para processamento assíncrono e desacoplado
 * de notificações à rede de proteção (cuidadores, familiares e serviços de emergência).
 *
 * @param alarmeId Identificador único do alarme persistido
 * @param idosoId Identificador único do idoso monitorado
 * @param nomeIdoso Nome completo do idoso
 * @param latitude Coordenada de latitude no momento do disparo
 * @param longitude Coordenada de longitude no momento do disparo
 * @param nivelBateria Nível percentual de bateria do dispositivo emissor
 * @param observacao Notas ou contexto do disparo
 * @param acionadoEm Timestamp exato do acionamento
 */
public record SosAcionadoEvent(
        UUID alarmeId,
        UUID idosoId,
        String nomeIdoso,
        Double latitude,
        Double longitude,
        Integer nivelBateria,
        String observacao,
        Instant acionadoEm
) {
}
