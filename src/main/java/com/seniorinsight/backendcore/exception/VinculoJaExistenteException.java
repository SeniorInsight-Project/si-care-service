package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando se tenta convidar ou vincular um familiar que já possui vínculo ativo com o idoso.
 */
public class VinculoJaExistenteException extends RuntimeException {

    public VinculoJaExistenteException(String message) {
        super(message);
    }
}
