package com.seniorinsight.backendcore.service;

import java.util.EnumMap;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.TipoConsentimento;
import com.seniorinsight.backendcore.dto.ConsentimentosDTO;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.mensageria.ConsentChanged;
import com.seniorinsight.backendcore.mensageria.PublicadorEventos;
import com.seniorinsight.backendcore.repository.ConsentimentoRepository;
import com.seniorinsight.backendcore.repository.ConsentimentoRepository.Decisao;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentimentoService {

    private static final String ACESSO_NEGADO = "Somente o idoso pode autorizar o uso dos próprios dados";

    private final ConsentimentoRepository repositorio;
    private final PublicadorEventos publicador;
    private final String dominioEmailIdoso;
    private final String versaoTermo;

    public ConsentimentoService(ConsentimentoRepository repositorio, PublicadorEventos publicador,
            @Value("${seniorinsight.idoso.dominio-email:idoso.seniorinsight.invalid}") String dominioEmailIdoso,
            @Value("${seniorinsight.consentimento.versao-termo:v1}") String versaoTermo) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.dominioEmailIdoso = dominioEmailIdoso;
        if (versaoTermo.isBlank() || versaoTermo.length() > 50) {
            throw new IllegalArgumentException("Versão do termo de consentimento inválida");
        }
        this.versaoTermo = versaoTermo;
    }

    @Transactional(readOnly = true)
    public ConsentimentosDTO consultar(UUID idosoId, Jwt jwt) {
        exigirTitularOuCurador(idosoId, jwt);
        if (!repositorio.existeIdoso(idosoId)) {
            throw new IdosoNaoEncontradoException(idosoId);
        }
        var autorizacoes = new EnumMap<TipoConsentimento, Boolean>(TipoConsentimento.class);
        repositorio.buscarAtuais(idosoId).forEach((tipo, decisao) -> autorizacoes.put(tipo, decisao.autorizado()));
        return ConsentimentosDTO.de(autorizacoes);
    }

    @Transactional
    public ConsentimentosDTO salvar(UUID idosoId, Jwt jwt, ConsentimentosDTO novas) {
        exigirTitularOuCurador(idosoId, jwt);
        if (!repositorio.bloquearIdoso(idosoId)) {
            throw new IdosoNaoEncontradoException(idosoId);
        }
        var atuais = repositorio.buscarAtuais(idosoId);
        for (TipoConsentimento tipo : TipoConsentimento.values()) {
            var decisao = new Decisao(novas.autorizado(tipo), versaoTermo);
            if (!decisao.equals(atuais.get(tipo))) {
                repositorio.registrar(idosoId, tipo, decisao, jwt.getSubject());
                publicador.publicar(new ConsentChanged(idosoId, tipo, ConsentimentoRepository.REDE,
                        decisao.autorizado()));
            }
        }
        return novas;
    }

    private void exigirTitularOuCurador(UUID idosoId, Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new AccessDeniedException(ACESSO_NEGADO);
        }

        UsuarioAutenticado usuario = UsuarioAutenticado.de(jwt);
        String contaTecnica = "idoso-" + idosoId + "@" + dominioEmailIdoso;
        boolean titular = usuario.temPapel("idoso")
                && contaTecnica.equalsIgnoreCase(jwt.getClaimAsString("preferred_username"))
                && contaTecnica.equalsIgnoreCase(usuario.email())
                && Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        if (!titular && !repositorio.curatelaAtiva(idosoId, jwt.getSubject())) {
            throw new AccessDeniedException(ACESSO_NEGADO);
        }
    }
}
