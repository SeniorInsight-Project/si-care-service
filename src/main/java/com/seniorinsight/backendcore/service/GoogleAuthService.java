package com.seniorinsight.backendcore.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.seniorinsight.backendcore.dto.AuthResponseDTO;
import com.seniorinsight.backendcore.exception.InvalidTokenException;
import com.seniorinsight.backendcore.model.ProvedorSso;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtService;

/**
 * Serviço responsável pela autenticação via Single Sign-On (Google) e
 * provisionamento Just-In-Time (JIT) de usuários (HU-001).
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final FamiliarRepository familiarRepository;
    private final MedicoRepository medicoRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(
            UsuarioRepository usuarioRepository,
            FamiliarRepository familiarRepository,
            MedicoRepository medicoRepository,
            JwtService jwtService,
            @Value("${google.client-id:}") String webClientId,
            @Value("${google.android-client-id:}") String androidClientId,
            @Value("${google.ios-client-id:}") String iosClientId) {
        this.usuarioRepository = usuarioRepository;
        this.familiarRepository = familiarRepository;
        this.medicoRepository = medicoRepository;
        this.jwtService = jwtService;

        java.util.List<String> validAudiences = java.util.stream.Stream.of(webClientId, androidClientId, iosClientId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .toList();

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(validAudiences.isEmpty() ? Collections.singletonList("") : validAudiences)
                .build();
    }

    /**
     * Autentica um usuário através do Google ID Token.
     * Caso o usuário não exista no banco, realiza o cadastro automático (JIT Provisioning).
     *
     * @param idTokenString Token JWT emitido pelo Google recebido do cliente
     * @return AuthResponseDTO contendo o access token JWT da aplicação e status de perfil
     */
    @Transactional
    public AuthResponseDTO authenticateWithGoogle(String idTokenString) {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new InvalidTokenException("O ID Token do Google não pode ser vazio.");
        }

        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Erro criptográfico ou de rede ao validar Google ID Token: {}", e.getMessage());
            throw new InvalidTokenException("Falha na validação do token junto ao Google.", e);
        }

        if (googleIdToken == null) {
            log.warn("Tentativa de login com Google ID Token inválido ou expirado.");
            throw new InvalidTokenException("Google ID Token inválido ou expirado.");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String ssoId = payload.getSubject();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        if (!emailVerified) {
            throw new InvalidTokenException("O e-mail da conta Google não foi verificado.");
        }

        // Busca o usuário pelo e-mail
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        Usuario usuario;
        boolean isNewUser = false;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
            // Vincula o ssoId se ainda não estiver configurado
            if (usuario.getSsoId() == null) {
                usuario.setSsoId(ssoId);
            }
            if (usuario.getProvedorSso() == ProvedorSso.LOCAL) {
                usuario.setProvedorSso(ProvedorSso.GOOGLE);
            }
            usuario = usuarioRepository.save(usuario);
            log.info("Usuário existente autenticado via SSO Google: {}", email);
        } else {
            // Provisionamento Just-In-Time (JIT)
            Usuario novoUsuario = Usuario.builder()
                    .email(email)
                    .provedorSso(ProvedorSso.GOOGLE)
                    .ssoId(ssoId)
                    .status(StatusUsuario.ATIVO)
                    .mfaHabilitado(false)
                    .build();

            usuario = usuarioRepository.save(novoUsuario);
            isNewUser = true;
            log.info("Novo usuário provisionado via SSO Google (JIT): {}", email);
        }

        String jwt = jwtService.generateToken(usuario);
        boolean perfilCompleto = familiarRepository.existsByUsuario(usuario) || medicoRepository.existsByUsuario(usuario);

        return new AuthResponseDTO(jwt, perfilCompleto);
    }
}
