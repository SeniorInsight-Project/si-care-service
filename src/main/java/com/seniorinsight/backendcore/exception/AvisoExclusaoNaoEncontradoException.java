package com.seniorinsight.backendcore.exception;

/** Não há aviso à rede para o protocolo: o pedido de exclusão ainda não foi processado, ou não era de um idoso. */
public class AvisoExclusaoNaoEncontradoException extends RuntimeException {

    public AvisoExclusaoNaoEncontradoException(String message) {
        super(message);
    }
}
