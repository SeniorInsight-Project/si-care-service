package com.seniorinsight.backendcore.domain;

/**
 * Enumeração representando os possíveis estados de um convite da Rede de Proteção (HU16).
 */
public enum StatusConvite {

    /**
     * Convite emitido e aguardando processamento pelo familiar destinatário dentro da janela de 48h.
     */
    PENDENTE,

    /**
     * Convite validado e aceito com sucesso, gerando o respectivo vínculo de rede.
     */
    ACEITO,

    /**
     * Convite que expirou após o transcurso da validade temporal de 48h sem ter sido aceito.
     */
    EXPIRADO,

    /**
     * Convite cancelado ou substituído por uma nova emissão para o mesmo destinatário.
     */
    CANCELADO
}
