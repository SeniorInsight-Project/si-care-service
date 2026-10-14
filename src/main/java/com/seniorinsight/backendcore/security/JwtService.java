package com.seniorinsight.backendcore.security;

import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.seniorinsight.backendcore.model.Usuario;

/**
 * Serviço de geração e validação de tokens JWT utilizando a biblioteca Auth0 java-jwt.
 */
@Service
public class JwtService {

    private static final String ISSUER = "seniorinsight-api";
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final String secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${api.security.jwt.secret:}") String secret,
            @Value("${api.security.jwt.expiration:86400}") long expirationSeconds) {
        if (secret == null || secret.isBlank() || secret.length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("JWT_SECRET não definido ou com menos de "
                    + TAMANHO_MINIMO_SEGREDO + " caracteres. Gere um valor com: openssl rand -hex 32");
        }
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Gera um token JWT assinado com HMAC256 para o usuário autenticado.
     *
     * @param usuario Entidade do usuário
     * @return String contendo o token JWT
     */
    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.secret);
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(this.expirationSeconds);

            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getEmail())
                    .withClaim("userId", usuario.getId().toString())
                    .withClaim("provider", usuario.getProvedorSso().name())
                    .withClaim("status", usuario.getStatus().name())
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(expiresAt))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT de autenticação", exception);
        }
    }

    /**
     * Valida um token JWT e extrai o subject (e-mail).
     *
     * @param token Token JWT
     * @return e-mail contido no subject
     * @throws JWTVerificationException se o token for inválido ou expirado
     */
    public String validateAndGetSubject(String token) {
        Algorithm algorithm = Algorithm.HMAC256(this.secret);
        return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token)
                .getSubject();
    }
}
