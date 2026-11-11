package com.seniorinsight.backendcore.validation;

import java.util.Optional;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/** Validação e normalização de telefone com libphonenumber. Sem código de país, assume o Brasil. */
public final class Telefones {

    static final String REGIAO_PADRAO = "BR";

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private Telefones() {
    }

    /** Telefone em E.164 (ex.: {@code +5511987654321}), ou vazio se não for um número válido. */
    public static Optional<String> paraE164(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return Optional.empty();
        }
        try {
            PhoneNumber numero = UTIL.parse(telefone.trim(), REGIAO_PADRAO);
            if (!UTIL.isValidNumber(numero)) {
                return Optional.empty();
            }
            return Optional.of(UTIL.format(numero, PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }
}
