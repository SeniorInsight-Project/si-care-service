package com.seniorinsight.backendcore.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.exception.AlarmeNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.exception.RotinaMedicamentoNaoEncontradaException;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.MedicoRepository;
import com.seniorinsight.backendcore.repository.RotinaMedicamentoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

/**
 * Ponto único de autorização das rotas por idoso: confere se o usuário do token é da rede <b>daquele</b> idoso.
 *
 * <p>Tem acesso:
 * <ul>
 *   <li>o familiar com vínculo na rede de proteção do idoso ({@code vinculos_rede});</li>
 *   <li>o médico vinculado ao idoso ({@code medicos_idosos});</li>
 *   <li>o próprio idoso, pelo token do aparelho pareado. O identity-service cria o usuário do idoso no Keycloak
 *       com a role {@code idoso} e o e-mail técnico {@code idoso-<idosoId>@<dominio>} (ADR 0001 do
 *       si-identity-service), e é isso que identifica o idoso aqui.</li>
 * </ul>
 *
 * <p>Sem vínculo a resposta é 403, exista o idoso ou não, para não revelar quais idosos existem.
 */
@Service
public class AcessoIdosoService {

    static final String PAPEL_IDOSO = "idoso";

    private static final Logger log = LoggerFactory.getLogger(AcessoIdosoService.class);

    private final VinculoRedeRepository vinculoRedeRepository;
    private final MedicoRepository medicoRepository;
    private final AlarmeEmergenciaRepository alarmeEmergenciaRepository;
    private final RotinaMedicamentoRepository rotinaMedicamentoRepository;
    private final String dominioEmailIdoso;

    public AcessoIdosoService(
            VinculoRedeRepository vinculoRedeRepository,
            MedicoRepository medicoRepository,
            AlarmeEmergenciaRepository alarmeEmergenciaRepository,
            RotinaMedicamentoRepository rotinaMedicamentoRepository,
            @Value("${seniorinsight.idoso.dominio-email:idoso.seniorinsight.invalid}") String dominioEmailIdoso) {
        this.vinculoRedeRepository = vinculoRedeRepository;
        this.medicoRepository = medicoRepository;
        this.alarmeEmergenciaRepository = alarmeEmergenciaRepository;
        this.rotinaMedicamentoRepository = rotinaMedicamentoRepository;
        this.dominioEmailIdoso = dominioEmailIdoso;
    }

    /**
     * Exige que o usuário seja da rede do idoso.
     *
     * @throws PerfilNaoAutorizadoException sem token ou sem vínculo (403)
     * @throws IllegalArgumentException     sem {@code idosoId} (400)
     */
    @Transactional(readOnly = true)
    public void exigirVinculo(UsuarioAutenticado usuario, UUID idosoId) {
        if (usuario == null || usuario.keycloakSub() == null || usuario.keycloakSub().isBlank()) {
            throw new PerfilNaoAutorizadoException("Usuário não autenticado. Forneça um token JWT válido.");
        }
        if (idosoId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }
        if (!temVinculo(usuario, idosoId)) {
            log.warn("Acesso negado: usuário {} não é da rede do idoso {}.", usuario.keycloakSub(), idosoId);
            throw new PerfilNaoAutorizadoException("Você não tem vínculo com este idoso.");
        }
    }

    /**
     * Carrega o alarme e exige vínculo com o idoso dono dele.
     *
     * @throws AlarmeNaoEncontradoException alarme inexistente (404)
     */
    @Transactional(readOnly = true)
    public void exigirVinculoComAlarme(UsuarioAutenticado usuario, UUID alarmeId) {
        if (alarmeId == null) {
            throw new IllegalArgumentException("O identificador do alarme não pode ser nulo.");
        }
        UUID idosoId = alarmeEmergenciaRepository.findIdosoIdById(alarmeId)
                .orElseThrow(() -> new AlarmeNaoEncontradoException(alarmeId));
        exigirVinculo(usuario, idosoId);
    }

    /**
     * Carrega a rotina de medicamento e exige vínculo com o idoso dono dela.
     *
     * @throws RotinaMedicamentoNaoEncontradaException rotina inexistente (404)
     */
    @Transactional(readOnly = true)
    public void exigirVinculoComRotina(UsuarioAutenticado usuario, UUID rotinaId) {
        if (rotinaId == null) {
            throw new IllegalArgumentException("O identificador da rotina não pode ser nulo.");
        }
        UUID idosoId = rotinaMedicamentoRepository.findIdosoIdById(rotinaId)
                .orElseThrow(() -> new RotinaMedicamentoNaoEncontradaException(rotinaId));
        exigirVinculo(usuario, idosoId);
    }

    private boolean temVinculo(UsuarioAutenticado usuario, UUID idosoId) {
        String sub = usuario.keycloakSub();
        return ehOProprioIdoso(usuario, idosoId)
                || vinculoRedeRepository.existsByFamiliar_KeycloakSubAndIdoso_Id(sub, idosoId)
                || medicoRepository.existsByKeycloakSubAndIdosos_Id(sub, idosoId);
    }

    private boolean ehOProprioIdoso(UsuarioAutenticado usuario, UUID idosoId) {
        return usuario.temPapel(PAPEL_IDOSO)
                && usuario.email() != null
                && usuario.email().equalsIgnoreCase("idoso-" + idosoId + "@" + dominioEmailIdoso);
    }
}
