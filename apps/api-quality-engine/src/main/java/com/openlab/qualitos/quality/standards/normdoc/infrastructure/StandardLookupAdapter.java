package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardRepository;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter du port {@link NormDocStandardLookup} sur le catalogue normatif JPA
 * (platform-level, pas de tenant_id).
 */
@Component
public class StandardLookupAdapter implements NormDocStandardLookup {

    private final StandardRepository standards;

    public StandardLookupAdapter(StandardRepository standards) {
        this.standards = standards;
    }

    @Override
    public Optional<StandardRef> findById(UUID standardId) {
        return standards.findById(standardId)
                .map(this::toRef);
    }

    private StandardRef toRef(Standard s) {
        return new StandardRef(s.getId(), s.getCode(), s.getFullName());
    }
}
