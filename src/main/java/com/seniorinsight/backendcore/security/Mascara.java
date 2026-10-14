package com.seniorinsight.backendcore.security;

/**
 * Mascara dados pessoais antes de irem para o log (LGPD; RNF-017: logs sem identificadores diretos).
 */
public final class Mascara {

    private Mascara() {
    }

    /** Mantém só a primeira letra e o domínio: {@code maria.silva@gmail.com} vira {@code m***@gmail.com}. */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "(vazio)";
        }
        int arroba = email.indexOf('@');
        return arroba <= 0 ? "***" : email.charAt(0) + "***" + email.substring(arroba);
    }
}
