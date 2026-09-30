package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando o usuário autenticado não possui um perfil autorizado (Familiar ou Médico).
 */
public class PerfilNaoAutorizadoException extends RuntimeException {

    public PerfilNaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
