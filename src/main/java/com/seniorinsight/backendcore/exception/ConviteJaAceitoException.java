package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando o convite consultado já foi aceito.
 */
public class ConviteJaAceitoException extends RuntimeException {

    public ConviteJaAceitoException(String message) {
        super(message);
    }
}
