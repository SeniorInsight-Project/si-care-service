package com.seniorinsight.backendcore.exception;

public class LimiteContatosEmergenciaException extends RuntimeException {

    public LimiteContatosEmergenciaException(int limite) {
        super("O idoso já tem o máximo de " + limite + " contatos de emergência.");
    }
}
