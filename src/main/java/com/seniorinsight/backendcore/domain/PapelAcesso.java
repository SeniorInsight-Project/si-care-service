package com.seniorinsight.backendcore.domain;

/** Valores de {@code vinculos_rede.papel_acesso} que o care-service interpreta. */
public final class PapelAcesso {

    /** Familiar Administrador: exatamente um por idoso (RN-012); edita o cadastro e gere a rede. */
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    /** Grau de parentesco gravado no vínculo do autor, já que o cadastro do idoso não pergunta isso. */
    public static final String PARENTESCO_NAO_INFORMADO = "NAO_INFORMADO";

    private PapelAcesso() {
    }
}
