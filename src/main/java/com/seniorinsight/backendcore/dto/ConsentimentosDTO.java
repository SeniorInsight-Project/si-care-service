package com.seniorinsight.backendcore.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seniorinsight.backendcore.domain.TipoConsentimento;
import jakarta.validation.constraints.NotNull;

public record ConsentimentosDTO(
        @NotNull @JsonProperty("BATIMENTOS") Boolean batimentos,
        @NotNull @JsonProperty("PASSOS") Boolean passos,
        @NotNull @JsonProperty("SONO") Boolean sono,
        @NotNull @JsonProperty("OXIGENACAO") Boolean oxigenacao,
        @NotNull @JsonProperty("LOCALIZACAO") Boolean localizacao) {

    public boolean autorizado(TipoConsentimento tipo) {
        return switch (tipo) {
            case BATIMENTOS -> batimentos;
            case PASSOS -> passos;
            case SONO -> sono;
            case OXIGENACAO -> oxigenacao;
            case LOCALIZACAO -> localizacao;
        };
    }

    public static ConsentimentosDTO de(Map<TipoConsentimento, Boolean> atuais) {
        return new ConsentimentosDTO(
                atuais.getOrDefault(TipoConsentimento.BATIMENTOS, false),
                atuais.getOrDefault(TipoConsentimento.PASSOS, false),
                atuais.getOrDefault(TipoConsentimento.SONO, false),
                atuais.getOrDefault(TipoConsentimento.OXIGENACAO, false),
                atuais.getOrDefault(TipoConsentimento.LOCALIZACAO, false));
    }
}
