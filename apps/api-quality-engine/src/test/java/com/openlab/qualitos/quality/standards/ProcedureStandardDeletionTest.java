package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.docs.DocumentRepository;
import com.openlab.qualitos.quality.docs.DocumentVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Supprimer un référentiel efface aussi les clauses auxquelles des preuves et
 * des audits se rattachent. La seule question qui vaille est donc : à partir de
 * quand ce geste devient-il destructeur pour quelqu'un d'autre ?
 */
@ExtendWith(MockitoExtension.class)
class ProcedureStandardDeletionTest {

    @Mock StandardRepository standards;
    @Mock DocumentRepository documents;
    @Mock DocumentVersionRepository versions;
    @Mock TenantStandardRepository adoptions;

    ProcedureStandardService service;

    static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        service = new ProcedureStandardService(standards, documents, versions, adoptions);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private Standard owned() {
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        s.setOwnerTenantId(TENANT);
        s.setCode("PRO-002");
        return s;
    }

    @Test
    void deletesAReferentialNobodyFollows() {
        Standard s = owned();
        when(standards.findOwnedById(s.getId(), TENANT)).thenReturn(Optional.of(s));
        when(adoptions.existsByStandardId(s.getId())).thenReturn(false);

        service.deleteStandard(s.getId());

        // Sections, clauses et exigences partent avec lui : orphanRemoval est
        // déjà déclaré sur les collections, rien à orchestrer ici.
        verify(standards).delete(s);
    }

    @Test
    void refusesToDeleteAReferentialAProjectIsFollowing() {
        // Une adoption en cours, c'est un projet de conformité, ses preuves et
        // son score d'alignement. Le référentiel disparaîtrait sous ses pieds.
        Standard s = owned();
        when(standards.findOwnedById(s.getId(), TENANT)).thenReturn(Optional.of(s));
        when(adoptions.existsByStandardId(s.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.deleteStandard(s.getId()))
                .isInstanceOf(AdoptionConflictException.class);

        verify(standards, never()).delete(any(Standard.class));
    }

    @Test
    void refusesToDeleteAPlatformStandard() {
        UUID platformId = UUID.randomUUID();
        Standard platform = new Standard();
        platform.setId(platformId);
        when(standards.findOwnedById(platformId, TENANT)).thenReturn(Optional.empty());
        when(standards.findVisibleById(platformId, TENANT)).thenReturn(Optional.of(platform));

        assertThatThrownBy(() -> service.deleteStandard(platformId))
                .isInstanceOf(PlatformStandardWriteException.class);

        verify(standards, never()).delete(any(Standard.class));
    }

    @Test
    void treatsAnotherTenantsReferentialAsAbsent() {
        UUID other = UUID.randomUUID();
        when(standards.findOwnedById(other, TENANT)).thenReturn(Optional.empty());
        when(standards.findVisibleById(other, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteStandard(other))
                .isInstanceOf(StandardNotFoundException.class);

        verify(standards, never()).delete(any(Standard.class));
    }
}
