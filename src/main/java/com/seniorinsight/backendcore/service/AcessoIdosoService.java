package com.seniorinsight.backendcore.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.PapelAcesso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

/**
 * Autorização por idoso (RF-007): o usuário só acessa um idoso se estiver na rede dele (familiar com vínculo) ou se
 * for médico que o acompanha. Idoso inexistente responde 404; sem vínculo, 403.
 */
@Service
public class AcessoIdosoService {

    private final IdosoRepository idosoRepository;
    private final VinculoRedeRepository vinculoRedeRepository;
    private final String dominioEmailIdoso;

    public AcessoIdosoService(IdosoRepository idosoRepository, VinculoRedeRepository vinculoRedeRepository,
            @Value("${seniorinsight.idoso.dominio-email:idoso.seniorinsight.invalid}") String dominioEmailIdoso) {
        this.idosoRepository = idosoRepository;
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.dominioEmailIdoso = dominioEmailIdoso;
    }

    /**
     * Leitura: além de quem {@link #exigirVinculo} aceita, o próprio idoso, usando o token do aparelho pareado
     * (conta técnica {@code idoso-<id>@<domínio>} com a role {@code idoso}).
     */
    @Transactional(readOnly = true)
    public Idoso exigirLeitura(UsuarioAutenticado usuario, UUID idosoId) {
        Idoso idoso = carregar(idosoId);
        if (!idosoPareado(usuario, idosoId) && vinculoDe(usuario, idosoId) == null
                && !medicoAcompanha(usuario, idosoId)) {
            throw new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso.");
        }
        return idoso;
    }

    private boolean idosoPareado(UsuarioAutenticado usuario, UUID idosoId) {
        if (usuario == null || !usuario.temPapel("idoso")) {
            return false;
        }
        String contaTecnica = "idoso-" + idosoId + "@" + dominioEmailIdoso;
        return usuario.emailVerificado()
                && contaTecnica.equalsIgnoreCase(usuario.usernamePreferido())
                && contaTecnica.equalsIgnoreCase(usuario.email());
    }

    /** Exige que o usuário esteja na rede do idoso (qualquer papel) ou o acompanhe como médico. */
    @Transactional(readOnly = true)
    public Idoso exigirVinculo(UsuarioAutenticado usuario, UUID idosoId) {
        Idoso idoso = carregar(idosoId);
        if (vinculoDe(usuario, idosoId) == null && !medicoAcompanha(usuario, idosoId)) {
            throw new PerfilNaoAutorizadoException("Você não faz parte da rede de proteção deste idoso.");
        }
        return idoso;
    }

    /** Exige o vínculo de Familiar Administrador com o idoso. */
    @Transactional(readOnly = true)
    public Idoso exigirAdministrador(UsuarioAutenticado usuario, UUID idosoId) {
        Idoso idoso = carregar(idosoId);
        VinculoRede vinculo = vinculoDe(usuario, idosoId);
        if (vinculo == null || !PapelAcesso.ADMINISTRADOR.equals(vinculo.getPapelAcesso())) {
            throw new PerfilNaoAutorizadoException("Apenas o Familiar Administrador pode realizar esta operação.");
        }
        return idoso;
    }

    private Idoso carregar(UUID idosoId) {
        return idosoRepository.findById(idosoId).orElseThrow(() -> new IdosoNaoEncontradoException(idosoId));
    }

    private VinculoRede vinculoDe(UsuarioAutenticado usuario, UUID idosoId) {
        if (usuario == null || usuario.keycloakSub() == null) {
            return null;
        }
        return vinculoRedeRepository.findByFamiliar_KeycloakSubAndIdoso_Id(usuario.keycloakSub(), idosoId)
                .orElse(null);
    }

    private boolean medicoAcompanha(UsuarioAutenticado usuario, UUID idosoId) {
        return usuario != null && usuario.keycloakSub() != null
                && idosoRepository.medicoAcompanha(idosoId, usuario.keycloakSub());
    }
}
