package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando uma comorbidade específica não é encontrada na lista do idoso.
 */
public class ComorbidadeNaoEncontradaException extends RuntimeException {

    public ComorbidadeNaoEncontradaException(String nomeComorbidade) {
        super("Comorbidade '" + nomeComorbidade + "' não encontrada no perfil do idoso.");
    }
}
