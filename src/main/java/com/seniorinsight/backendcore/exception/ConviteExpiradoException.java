package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando a tentativa de aceite de convite ocorre após o prazo de expiração de 48h.
 */
public class ConviteExpiradoException extends RuntimeException {

    public ConviteExpiradoException(String message) {
        super(message);
    }
}
