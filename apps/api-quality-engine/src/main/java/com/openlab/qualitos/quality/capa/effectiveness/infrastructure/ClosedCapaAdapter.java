package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.effectiveness.application.ClosedCapaPort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les dossiers clos du tenant, chacun avec la signature de son probleme.
 *
 * <p>La signature se lit sur la non-conformite d'origine, retrouvee par la
 * requete que le module NC expose deja. Elle n'est pas stockee sur la CAPA a
 * dessein : la recopier la figerait, alors que le rattachement d'une NC a un
 * mode de defaillance peut etre corrige apres coup — et doit alors corriger la
 * mesure.
 */
@Component
public class ClosedCapaAdapter implements ClosedCapaPort {

    private final CapaCaseRepository capaCases;
    private final NonConformityRepository nonConformities;

    public ClosedCapaAdapter(CapaCaseRepository capaCases, NonConformityRepository nonConformities) {
        this.capaCases = capaCases;
        this.nonConformities = nonConformities;
    }

    /**
     * Les dossiers clos QUI ATTENDENT UN VERDICT.
     *
     * <p>Un dossier dont on a decide a l'ouverture qu'aucune verification
     * n'etait exigee n'a rien a faire ici : il est clos, point. Il figurait
     * pourtant dans le registre, en attente d'une evaluation que personne ne
     * prononcerait jamais -- ce qui gonflait le retard apparent avec des
     * dossiers deliberement sortis du perimetre, et noyait ceux qui attendent
     * vraiment.
     *
     * <p>Seul {@code FALSE} exclut. {@code null} veut dire « pas encore
     * tranche », et ne pas avoir repondu n'est pas avoir repondu non : ces
     * dossiers-la restent visibles, parce que c'est precisement le registre qui
     * fait apparaitre la question oubliee.
     */
    @Override
    public List<ClosedCapa> findClosed(UUID tenantId) {
        return capaCases.findByTenantIdAndStatusOrderByClosedAtDesc(tenantId, CapaStatus.CLOSED)
                .stream()
                .filter(capa -> !Boolean.FALSE.equals(capa.getVerificationRequired()))
                .map(capa -> toPort(tenantId, capa))
                .toList();
    }

    private ClosedCapa toPort(UUID tenantId, CapaCase capa) {
        return new ClosedCapa(
                capa.getId(),
                capa.getTitle(),
                capa.getCriticity() == null ? null : capa.getCriticity().name(),
                capa.getCreatedAt(),
                capa.getClosedAt(),
                capa.getEffectivenessVerified(),
                signatureOf(tenantId, capa));
    }

    private RecurrenceSignature signatureOf(UUID tenantId, CapaCase capa) {
        Optional<NonConformity> source =
                nonConformities.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(tenantId, capa.getId());
        return source
                .map(nc -> RecurrenceSignature.of(nc.getProductId(), nc.getFmeaItemId(),
                        nc.getCategory() == null ? null : nc.getCategory().name()))
                .orElse(RecurrenceSignature.NONE);
    }
}
