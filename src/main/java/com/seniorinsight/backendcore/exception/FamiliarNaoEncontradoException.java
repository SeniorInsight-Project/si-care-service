package com.seniorinsight.backendcore.exception;

import java.util.UUID;

/** Familiar não localizado (HTTP 404). */
public class FamiliarNaoEncontradoException extends RuntimeException {

    public FamiliarNaoEncontradoException(UUID familiarId) {
        super("Familiar não encontrado com o identificador: " + familiarId);
    }
}
