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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La saisie de l'arborescence d'un référentiel de procédure.
 *
 * <p>Trois issues à distinguer devant un identifiant de référentiel, et la
 * distinction porte du sens : c'est le mien (j'édite), c'est une norme de la
 * plateforme (403 — elle existe, mais son contenu vient des migrations), ou ce
 * n'est rien du tout (404 — y compris le référentiel d'un AUTRE tenant, dont on
 * ne confirme pas l'existence).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcedureStandardEditingTest {

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
        // Le fournisseur de persistance attribue les identifiants des nouveaux
        // nœuds ; on le simule, sans quoi aucun test ne pourrait désigner ce
        // qu'il vient de créer — et c'est bien ainsi que le front procède :
        // il crée, puis relit la fiche pour connaître les identifiants.
        when(standards.save(any(Standard.class))).thenAnswer(i -> {
            Standard saved = i.getArgument(0);
            saved.getSections().forEach(sec -> {
                assignIdIfMissing(sec.getId(), sec::setId);
                sec.getClauses().forEach(cl -> {
                    assignIdIfMissing(cl.getId(), cl::setId);
                    cl.getRequirements().forEach(r -> assignIdIfMissing(r.getId(), r::setId));
                });
            });
            return saved;
        });
    }

    private static void assignIdIfMissing(UUID current, java.util.function.Consumer<UUID> setter) {
        if (current == null) {
            setter.accept(UUID.randomUUID());
        }
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private Standard ownedStandard() {
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        s.setCode("PRO-002");
        s.setFullName("Audit interne");
        s.setOwnerTenantId(TENANT);
        s.setStatus(StandardStatus.PUBLISHED);
        when(standards.findOwnedById(s.getId(), TENANT)).thenReturn(Optional.of(s));
        return s;
    }

    private Standard platformStandard() {
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        s.setCode("iso-9001");
        s.setStatus(StandardStatus.PUBLISHED);
        return s;
    }

    private ProcedureStandardDto.SectionRequest section(String code, String title) {
        return new ProcedureStandardDto.SectionRequest(code, title, null);
    }

    private ProcedureStandardDto.ClauseRequest clause(String code, String title) {
        return new ProcedureStandardDto.ClauseRequest(code, title, null);
    }

    private ProcedureStandardDto.RequirementRequest requirement(String code, String text) {
        return new ProcedureStandardDto.RequirementRequest(
                code, text, ObligationLevel.MUST, "compte-rendu", "signé", RiskLevel.HIGH);
    }

    private static <T> T last(List<T> items) {
        return items.get(items.size() - 1);
    }

    // ---- Sections ----

    @Test
    void addsASectionAtTheEndOfTheTree() {
        Standard s = ownedStandard();

        service.addSection(s.getId(), section("1", "Programmation"));
        service.addSection(s.getId(), section("2", "Réalisation"));

        assertThat(s.getSections()).extracting(StandardSection::getCode).containsExactly("1", "2");
        assertThat(s.getSections()).extracting(StandardSection::getOrderIndex).containsExactly(0, 1);
        assertThat(s.getSections().get(0).getStandard()).isSameAs(s);
    }

    @Test
    void updatesASection() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection created = last(s.getSections());

        service.updateSection(s.getId(), created.getId(),
                new ProcedureStandardDto.SectionRequest("1", "Programmation des audits", "portée"));

        assertThat(created.getTitle()).isEqualTo("Programmation des audits");
        assertThat(created.getDescription()).isEqualTo("portée");
    }

    @Test
    void deletesASectionAndEverythingUnderIt() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());
        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));

        service.deleteSection(s.getId(), sec.getId());

        assertThat(s.getSections()).isEmpty();
    }

    @Test
    void refusesTwoSectionsSharingACode() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));

        assertThatThrownBy(() -> service.addSection(s.getId(), section("1", "Doublon")))
                .isInstanceOf(StandardCodeConflictException.class);
    }

    /** Renommer une section sans changer son code ne doit pas se heurter à elle-même. */
    @Test
    void letsASectionKeepItsOwnCodeWhenRenamed() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection created = last(s.getSections());

        service.updateSection(s.getId(), created.getId(), section("1", "Autre titre"));

        assertThat(created.getTitle()).isEqualTo("Autre titre");
    }

    @Test
    void refusesToMoveASectionOntoAnotherSectionsCode() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        service.addSection(s.getId(), section("2", "Réalisation"));
        StandardSection second = last(s.getSections());

        assertThatThrownBy(() -> service.updateSection(s.getId(), second.getId(), section("1", "X")))
                .isInstanceOf(StandardCodeConflictException.class);
    }

    @Test
    void reportsAnUnknownSection() {
        Standard s = ownedStandard();
        UUID ghost = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateSection(s.getId(), ghost, section("1", "X")))
                .isInstanceOf(SectionNotFoundException.class);
        assertThatThrownBy(() -> service.deleteSection(s.getId(), ghost))
                .isInstanceOf(SectionNotFoundException.class);
        assertThatThrownBy(() -> service.addClause(s.getId(), ghost, clause("1.1", "X")))
                .isInstanceOf(SectionNotFoundException.class);
    }

    // ---- Clauses ----

    @Test
    void addsAClauseUnderItsSection() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());

        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));
        service.addClause(s.getId(), sec.getId(), clause("1.2", "Périmètre"));

        assertThat(sec.getClauses()).extracting(StandardClause::getCode).containsExactly("1.1", "1.2");
        assertThat(last(sec.getClauses()).getOrderIndex()).isEqualTo(1);
        assertThat(last(sec.getClauses()).getSection()).isSameAs(sec);
    }

    @Test
    void updatesAndDeletesAClause() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());
        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));
        StandardClause cl = last(sec.getClauses());

        service.updateClause(s.getId(), cl.getId(),
                new ProcedureStandardDto.ClauseRequest("1.1", "Fréquence des audits", "annuelle"));
        assertThat(cl.getTitle()).isEqualTo("Fréquence des audits");
        assertThat(cl.getDescription()).isEqualTo("annuelle");

        service.deleteClause(s.getId(), cl.getId());
        assertThat(sec.getClauses()).isEmpty();
    }

    /**
     * Le code d'une clause n'est unique QUE dans sa section : deux sections
     * peuvent légitimement numéroter leur première clause de la même façon.
     */
    @Test
    void scopesClauseCodeUniquenessToItsSection() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection one = last(s.getSections());
        service.addSection(s.getId(), section("2", "Réalisation"));
        StandardSection two = last(s.getSections());

        service.addClause(s.getId(), one.getId(), clause("1", "Fréquence"));
        service.addClause(s.getId(), two.getId(), clause("1", "Déroulé"));

        assertThat(one.getClauses()).hasSize(1);
        assertThat(two.getClauses()).hasSize(1);
        assertThatThrownBy(() -> service.addClause(s.getId(), one.getId(), clause("1", "Doublon")))
                .isInstanceOf(StandardCodeConflictException.class);
    }

    @Test
    void reportsAnUnknownClause() {
        Standard s = ownedStandard();
        UUID ghost = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateClause(s.getId(), ghost, clause("1.1", "X")))
                .isInstanceOf(ClauseNotFoundException.class);
        assertThatThrownBy(() -> service.deleteClause(s.getId(), ghost))
                .isInstanceOf(ClauseNotFoundException.class);
        assertThatThrownBy(() -> service.addRequirement(s.getId(), ghost, requirement("1.1.1", "X")))
                .isInstanceOf(ClauseNotFoundException.class);
    }

    // ---- Exigences ----

    @Test
    void addsARequirementUnderItsClause() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());
        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));
        StandardClause cl = last(sec.getClauses());

        service.addRequirement(s.getId(), cl.getId(),
                requirement("1.1.1", "Le programme est revu chaque année"));

        StandardRequirement r = last(cl.getRequirements());
        assertThat(cl.getRequirements()).hasSize(1);
        assertThat(r.getText()).isEqualTo("Le programme est revu chaque année");
        assertThat(r.getObligation()).isEqualTo(ObligationLevel.MUST);
        assertThat(r.getEvidenceTypes()).isEqualTo("compte-rendu");
        assertThat(r.getMeasurableCriteria()).isEqualTo("signé");
        assertThat(r.getRiskIfMissing()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.getOrderIndex()).isZero();
        assertThat(r.getClause()).isSameAs(cl);
    }

    @Test
    void updatesAndDeletesARequirement() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());
        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));
        StandardClause cl = last(sec.getClauses());
        service.addRequirement(s.getId(), cl.getId(),
                requirement("1.1.1", "Le programme est revu chaque année"));
        StandardRequirement r = last(cl.getRequirements());

        service.updateRequirement(s.getId(), r.getId(), new ProcedureStandardDto.RequirementRequest(
                "1.1.1", "Le programme est revu chaque semestre",
                ObligationLevel.SHOULD, null, null, null));

        assertThat(r.getText()).isEqualTo("Le programme est revu chaque semestre");
        assertThat(r.getObligation()).isEqualTo(ObligationLevel.SHOULD);
        // Les champs facultatifs se VIDENT quand la requête les omet : sans quoi
        // une preuve attendue effacée resterait affichée, et l'écran mentirait.
        assertThat(r.getEvidenceTypes()).isNull();
        assertThat(r.getMeasurableCriteria()).isNull();
        assertThat(r.getRiskIfMissing()).isNull();

        service.deleteRequirement(s.getId(), r.getId());
        assertThat(cl.getRequirements()).isEmpty();
    }

    @Test
    void refusesTwoRequirementsSharingACodeInTheSameClause() {
        Standard s = ownedStandard();
        service.addSection(s.getId(), section("1", "Programmation"));
        StandardSection sec = last(s.getSections());
        service.addClause(s.getId(), sec.getId(), clause("1.1", "Fréquence"));
        StandardClause cl = last(sec.getClauses());
        service.addRequirement(s.getId(), cl.getId(), requirement("1.1.1", "Texte"));

        assertThatThrownBy(() -> service.addRequirement(s.getId(), cl.getId(),
                requirement("1.1.1", "Doublon")))
                .isInstanceOf(StandardCodeConflictException.class);
    }

    @Test
    void reportsAnUnknownRequirement() {
        Standard s = ownedStandard();
        UUID ghost = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateRequirement(s.getId(), ghost, requirement("1", "X")))
                .isInstanceOf(RequirementNotFoundException.class);
        assertThatThrownBy(() -> service.deleteRequirement(s.getId(), ghost))
                .isInstanceOf(RequirementNotFoundException.class);
    }

    // ---- Garde d'accès, commun aux neuf opérations ----

    @Test
    void refusesToTouchAPlatformStandard() {
        // findOwnedById ne rend rien pour une norme plateforme ; on distingue
        // néanmoins les deux cas, car les messages ne disent pas la même chose.
        Standard platform = platformStandard();
        when(standards.findOwnedById(platform.getId(), TENANT)).thenReturn(Optional.empty());
        when(standards.findVisibleById(platform.getId(), TENANT)).thenReturn(Optional.of(platform));

        assertThatThrownBy(() -> service.addSection(platform.getId(), section("1", "X")))
                .isInstanceOf(PlatformStandardWriteException.class);
    }

    @Test
    void treatsAnotherTenantsReferentialAsAbsent() {
        UUID other = UUID.randomUUID();
        when(standards.findOwnedById(other, TENANT)).thenReturn(Optional.empty());
        when(standards.findVisibleById(other, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSection(other, section("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
    }

    /**
     * Le garde vaut pour les NEUF opérations, pas seulement pour la première :
     * une seule d'entre elles qui l'oublierait rouvrirait l'écriture sur le
     * référentiel d'un autre tenant.
     */
    @Test
    void guardsEverySingleWriteOperation() {
        UUID other = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        when(standards.findOwnedById(other, TENANT)).thenReturn(Optional.empty());
        when(standards.findVisibleById(other, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSection(other, child, section("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.deleteSection(other, child))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.addClause(other, child, clause("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.updateClause(other, child, clause("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.deleteClause(other, child))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.addRequirement(other, child, requirement("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.updateRequirement(other, child, requirement("1", "X")))
                .isInstanceOf(StandardNotFoundException.class);
        assertThatThrownBy(() -> service.deleteRequirement(other, child))
                .isInstanceOf(StandardNotFoundException.class);
    }
}
