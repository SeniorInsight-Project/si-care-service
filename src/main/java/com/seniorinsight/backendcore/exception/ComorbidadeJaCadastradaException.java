package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando a comorbidade informada já consta na lista de comorbidades do idoso.
 */
public class ComorbidadeJaCadastradaException extends RuntimeException {

    public ComorbidadeJaCadastradaException(String mensagem) {
        super(mensagem);
    }
}
