package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para autenticação via Single Sign-On (SSO)")
public record SsoLoginRequestDTO(
        @Schema(description = "Provedor de identidade SSO", example = "GOOGLE", allowableValues = {"GOOGLE", "APPLE"})
        @NotBlank(message = "O provedor é obrigatório.")
        String provider,

        @Schema(description = "ID Token JWT retornado pelo SDK do Google após o login no cliente")
        @NotBlank(message = "O idToken é obrigatório.")
        String idToken
) {}
