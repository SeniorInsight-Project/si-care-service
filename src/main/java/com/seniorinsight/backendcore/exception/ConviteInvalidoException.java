package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando o token de convite informado é inválido, inexistente ou já utilizado.
 */
public class ConviteInvalidoException extends RuntimeException {

    public ConviteInvalidoException(String message) {
        super(message);
    }
}
