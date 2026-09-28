package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando um token externo (ex: Google ID Token) é inválido, expirado ou forjado.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
