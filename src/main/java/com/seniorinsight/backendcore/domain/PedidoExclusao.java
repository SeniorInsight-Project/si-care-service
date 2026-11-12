package com.seniorinsight.backendcore.domain;

import java.util.UUID;

/**
 * Pedido de exclusão dos dados de um titular, já validado.
 *
 * @param protocolo  protocolo do report-service; identifica o pedido e é o {@code subject} da confirmação
 * @param tipo       idoso ou familiar
 * @param titularId  IDOSO: id do idoso no care (ou o id do perfil no identity, se ele não tem cadastro aqui);
 *                   FAMILIAR: id do perfil no identity, que o care não conhece
 * @param titularSub {@code sub} do Keycloak do titular; é por ele que o familiar é localizado
 */
public record PedidoExclusao(UUID protocolo, TipoTitular tipo, UUID titularId, String titularSub) {
}
