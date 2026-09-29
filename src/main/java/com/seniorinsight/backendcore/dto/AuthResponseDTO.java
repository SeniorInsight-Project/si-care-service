package com.seniorinsight.backendcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação bem-sucedida")
public record AuthResponseDTO(
        @Schema(description = "Token de acesso JWT emitido pelo SeniorInsight", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Tipo de autorização", example = "Bearer")
        String tokenType,

        @Schema(description = "Indica se o perfil do usuário está completo ou se necessita preenchimento inicial", example = "false")
        boolean perfilCompleto
) {
    public AuthResponseDTO(String accessToken, boolean perfilCompleto) {
        this(accessToken, "Bearer", perfilCompleto);
    }
}
