package com.seniorinsight.backendcore.exception;

import java.util.UUID;

/**
 * Exceção lançada quando uma rotina de medicamento não é encontrada pelo identificador.
 */
public class RotinaMedicamentoNaoEncontradaException extends RuntimeException {

    public RotinaMedicamentoNaoEncontradaException(UUID rotinaId) {
        super("Rotina de medicamento não encontrada com o identificador: " + rotinaId);
    }

    public RotinaMedicamentoNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
