package com.seniorinsight.backendcore.exception;

import java.util.UUID;

public class ContatoEmergenciaNaoEncontradoException extends RuntimeException {

    public ContatoEmergenciaNaoEncontradoException(UUID contatoId) {
        super("Contato de emergência não encontrado com o identificador: " + contatoId);
    }
}
