package com.seniorinsight.backendcore.exception;

/** O CPF informado já pertence a outro idoso (HTTP 409). */
public class CpfJaCadastradoException extends RuntimeException {

    public CpfJaCadastradoException() {
        super("Já existe um idoso cadastrado com este CPF.");
    }
}
