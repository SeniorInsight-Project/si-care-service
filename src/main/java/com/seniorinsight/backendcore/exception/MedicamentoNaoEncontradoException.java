package com.seniorinsight.backendcore.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um medicamento não é encontrado pelo identificador.
 */
public class MedicamentoNaoEncontradoException extends RuntimeException {

    public MedicamentoNaoEncontradoException(UUID medicamentoId) {
        super("Medicamento não encontrado com o identificador: " + medicamentoId);
    }

    public MedicamentoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
