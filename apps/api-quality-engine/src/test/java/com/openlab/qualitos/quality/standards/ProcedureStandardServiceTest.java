package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.docs.Document;
import com.openlab.qualitos.quality.docs.DocumentNotFoundException;
import com.openlab.qualitos.quality.docs.DocumentRepository;
import com.openlab.qualitos.quality.docs.DocumentType;
import com.openlab.qualitos.quality.docs.DocumentVersion;
import com.openlab.qualitos.quality.docs.DocumentVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un référentiel de procédure ne s'invente pas : il naît d'une procédure
 * APPROUVÉE de la GED, dont il hérite le code, le titre et la version. Un
 * référentiel sans procédure derrière lui n'audite rien d'opposable.
 */
@ExtendWith(MockitoExtension.class)
class ProcedureStandardServiceTest {

    @Mock StandardRepository standards;
    @Mock DocumentRepository documents;
    @Mock DocumentVersionRepository versions;

    ProcedureStandardService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final UUID VERSION = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        service = new ProcedureStandardService(standards, documents, versions);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private Document procedure() {
        Document d = new Document();
        d.setId(DOC);
        d.setTenantId(TENANT);
        d.setCode("PRO-002");
        d.setTitle("Audit interne");
        d.setType(DocumentType.PROCEDURE);
        d.setCurrentVersionId(VERSION);
        return d;
    }

    private DocumentVersion publishedVersion(int number) {
        DocumentVersion v = new DocumentVersion();
        v.setId(VERSION);
        v.setVersionNumber(number);
        return v;
    }

    @Test
    void createsAnEmptyReferentialCarryingTheProceduresIdentity() {
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(procedure()));
        when(versions.findByIdAndDocumentId(VERSION, DOC)).thenReturn(Optional.of(publishedVersion(3)));
        when(standards.existsBySourceDocument(DOC)).thenReturn(false);
        when(standards.save(any(Standard.class))).thenAnswer(i -> i.getArgument(0));

        service.createFromDocument(DOC);

        ArgumentCaptor<Standard> saved = ArgumentCaptor.forClass(Standard.class);
        verify(standards).save(saved.capture());
        Standard s = saved.getValue();
        assertThat(s.getOwnerTenantId()).isEqualTo(TENANT);
        assertThat(s.getCode()).isEqualTo("PRO-002");
        assertThat(s.getFullName()).isEqualTo("Audit interne");
        assertThat(s.getSourceDocumentId()).isEqualTo(DOC);
        assertThat(s.getFamily()).isEqualTo("INTERNAL_PROCEDURE");
        assertThat(s.getStatus()).isEqualTo(StandardStatus.PUBLISHED);
        assertThat(s.isCertificationBodyRequired()).isFalse();
        // VIDE : c'est tout l'objet de la fonctionnalité — le tenant saisit
        // lui-même les clauses de sa procédure.
        assertThat(s.getSections()).isEmpty();
    }

    /**
     * La version auditée doit rester lisible des années plus tard : un auditeur
     * demande contre QUELLE version de la procédure l'audit a été mené. Figer le
     * numéro à la création est la seule réponse, la procédure continuant d'évoluer.
     */
    @Test
    void freezesTheVersionOfTheProcedureItWasBornFrom() {
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(procedure()));
        when(versions.findByIdAndDocumentId(VERSION, DOC)).thenReturn(Optional.of(publishedVersion(3)));
        when(standards.existsBySourceDocument(DOC)).thenReturn(false);
        when(standards.save(any(Standard.class))).thenAnswer(i -> i.getArgument(0));

        service.createFromDocument(DOC);

        ArgumentCaptor<Standard> saved = ArgumentCaptor.forClass(Standard.class);
        verify(standards).save(saved.capture());
        assertThat(saved.getValue().getSourceDocumentVersion()).isEqualTo(3);
        // Et non "v1" : le référentiel affiche la version qu'il audite réellement.
        assertThat(saved.getValue().getCurrentVersion()).isEqualTo("v3");
    }

    @Test
    void refusesADocumentThatIsNotAProcedure() {
        Document d = procedure();
        d.setType(DocumentType.RECORD);
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(ProcedureSourceException.class)
                .hasMessageContaining("procédure");
        verify(standards, never()).save(any());
    }

    @Test
    void refusesAProcedureThatWasNeverApproved() {
        // Sans version publiée, la procédure n'est qu'un brouillon : auditer
        // contre un brouillon ne prouve rien.
        Document d = procedure();
        d.setCurrentVersionId(null);
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(ProcedureSourceException.class)
                .hasMessageContaining("approuvée");
        verify(standards, never()).save(any());
    }

    /**
     * Anomalie de données : le document pointe une version introuvable. On refuse
     * plutôt que de créer un référentiel dont la version source serait vide — il
     * prétendrait auditer une procédure sans pouvoir dire laquelle.
     */
    @Test
    void refusesWhenThePublishedVersionCannotBeResolved() {
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(procedure()));
        when(versions.findByIdAndDocumentId(VERSION, DOC)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(ProcedureSourceException.class)
                .hasMessageContaining("version");
        verify(standards, never()).save(any());
    }

    @Test
    void refusesASecondReferentialForTheSameProcedure() {
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.of(procedure()));
        when(versions.findByIdAndDocumentId(VERSION, DOC)).thenReturn(Optional.of(publishedVersion(1)));
        when(standards.existsBySourceDocument(DOC)).thenReturn(true);

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(StandardCodeConflictException.class);
        verify(standards, never()).save(any());
    }

    @Test
    void treatsAnotherTenantsDocumentAsAbsent() {
        // 404 et non 403 : un refus explicite confirmerait que ce document existe.
        when(documents.findByIdAndTenantId(DOC, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void refusesToWriteWithoutATenantInTheToken() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.createFromDocument(DOC))
                .isInstanceOf(MissingTenantContextException.class);
    }
}
