package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.effectiveness.application.NcOccurrencePort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte les non-conformites correspondant a une signature, sur un intervalle.
 *
 * <p>L'intervalle est ferme a gauche et OUVERT a droite. Une non-conformite
 * detectee a l'instant meme de l'ouverture du dossier appartient a la periode
 * « avant » et pas aux deux : un defaut compte une fois.
 */
@Component
public class NcOccurrenceAdapter implements NcOccurrencePort {

    private final NonConformityRepository nonConformities;

    public NcOccurrenceAdapter(NonConformityRepository nonConformities) {
        this.nonConformities = nonConformities;
    }

    @Override
    public int countBetween(UUID tenantId, RecurrenceSignature signature, Instant from, Instant to) {
        if (!to.isAfter(from)) {
            return 0;
        }
        if (signature.isPrecise()) {
            return (int) nonConformities
                    .countByTenantIdAndProductIdAndFmeaItemIdAndDetectedAtGreaterThanEqualAndDetectedAtLessThan(
                            tenantId, signature.productId(), signature.fmeaItemId(), from, to);
        }
        if (signature.category() == null) {
            return 0;
        }
        return (int) nonConformities
                .countByTenantIdAndCategoryAndDetectedAtGreaterThanEqualAndDetectedAtLessThan(
                        tenantId, NcCategory.valueOf(signature.category()), from, to);
    }
}
