package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IshikawaServiceTest {

    @Mock
    private IshikawaDiagramRepository diagramRepository;

    @Mock
    private IshikawaCauseRepository causeRepository;

    @Mock
    private AiGatewayClient ai;

    @InjectMocks
    private IshikawaService ishikawaService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OTHER_TENANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.setTenantId(TENANT_ID.toString());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // --- suggestCauses (IA) ---

    @Test
    void suggestCauses_parsesByCategory_respectsMode_andDedups() {
        UUID id = UUID.randomUUID();
        IshikawaDiagram d = new IshikawaDiagram();
        d.setTenantId(TENANT_ID);
        d.setMode(IshikawaMode.SIX_M);
        d.setProblemStatement("Pourquoi le taux de défauts augmente ?");
        when(diagramRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(d));

        // Format réel observé du petit modèle : « {CATEGORY} - cause » + ligne d'écho.
        String llm = String.join("\n",
                "CODE | cause courte (max 12 mots)",
                "{METHODS} - Procédure de contrôle obsolète",
                "{MANPOWER} - Formation insuffisante des opérateurs",
                "{MANAGEMENT} - Hors mode 6M, doit être ignoré",
                "{METHODS} - Procédure de contrôle obsolète");
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(llm, "ollama", 100, 1000));

        List<IshikawaDto.SuggestedCause> res = ishikawaService.suggestCauses(id);

        assertThat(res).hasSize(2);
        assertThat(res).extracting(IshikawaDto.SuggestedCause::category)
                .containsExactly(CauseCategory.METHODS, CauseCategory.MANPOWER);
        assertThat(res).noneMatch(s -> s.category() == CauseCategory.MANAGEMENT);
    }

    @Test
    void suggestCauses_recognizesEverySynonym_andTolerantFormats() {
        UUID id = UUID.randomUUID();
        IshikawaDiagram d = new IshikawaDiagram();
        d.setTenantId(TENANT_ID);
        d.setMode(IshikawaMode.EIGHT_M);          // toutes catégories autorisées
        d.setProblemStatement("Pourquoi ?");
        d.setDescription(null);                    // couvre la branche description == null
        when(diagramRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(d));

        // Une ligne par synonyme FR/EN de chaque catégorie + formats variés.
        // Libellé identique partout : la déduplication garde 1 seule suggestion,
        // donc le plafond n'est jamais atteint et categoryFromToken s'exécute pour TOUS les tokens.
        String llm = String.join("\n",
                "texte d'introduction sans catégorie",   // current null + splitCauseLine null
                "{METHODS} cause identique",
                "{MÉTHODES} cause identique",
                "Méthodes : cause identique",            // séparateur (i>0) au lieu d'accolades
                ":cause identique",                       // séparateur en position 0 (i>0 faux)
                "{MANPOWER} cause identique",
                "{MAIN} cause identique",
                "{ŒUVRE} cause identique",
                "{OEUVRE} cause identique",
                "{PERSONNEL} cause identique",
                "{MACHINE} cause identique",
                "{MATÉRIEL} cause identique",
                "{MATERIEL} cause identique",
                "{ÉQUIPEMENT} cause identique",
                "{EQUIPEMENT} cause identique",
                "{MATERIAL} cause identique",
                "{MATIÈRE} cause identique",
                "{MATIERE} cause identique",
                "{MEASURE} cause identique",
                "{MESURE} cause identique",
                "{ENVIRONNEMENT} cause identique",
                "{MILIEU} cause identique",
                "{MANAGEMENT} cause identique",
                "{MANAGE} cause identique",
                "{MONEY} cause identique",
                "{MOYENS} cause identique",
                "{ARGENT} cause identique",
                "{FINANCE} cause identique",
                "{INCONNU} cause identique");            // token non reconnu -> categoryFromToken == null
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(llm, "ollama", 100, 1000));

        List<IshikawaDto.SuggestedCause> res = ishikawaService.suggestCauses(id);

        // Dédupliqué : une seule suggestion, première catégorie reconnue = METHODS.
        assertThat(res).hasSize(1);
        assertThat(res.get(0).category()).isEqualTo(CauseCategory.METHODS);
        assertThat(res.get(0).label()).isEqualTo("cause identique");
    }

    @Test
    void suggestCauses_capsAtMax_skipsExisting_andDeduplicates() {
        UUID id = UUID.randomUUID();
        IshikawaDiagram d = new IshikawaDiagram();
        d.setTenantId(TENANT_ID);
        d.setMode(IshikawaMode.SIX_M);
        d.setProblemStatement("Pb");
        IshikawaCause known = buildCause(d, CauseCategory.METHODS);
        known.setLabel("déjà connu");
        d.getCauses().add(known);
        when(diagramRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(d));

        StringBuilder sb = new StringBuilder();
        sb.append("{METHODS} déjà connu\n");          // existing -> ignoré
        sb.append("{METHODS} doublon\n");
        sb.append("{METHODS} doublon\n");             // dédupliqué
        for (int i = 0; i < 15; i++) {                // largement plus que le plafond (12)
            sb.append("{MANPOWER} cause numéro ").append(i).append("\n");
        }
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(sb.toString(), "ollama", 100, 1000));

        List<IshikawaDto.SuggestedCause> res = ishikawaService.suggestCauses(id);

        assertThat(res).hasSize(12);                  // plafonné
        assertThat(res).noneMatch(s -> s.label().equals("déjà connu"));
    }

    @Test
    void suggestCauses_headerThenBulletList_attachesToCurrentCategory() {
        UUID id = UUID.randomUUID();
        IshikawaDiagram d = new IshikawaDiagram();
        d.setTenantId(TENANT_ID);
        d.setMode(IshikawaMode.SIX_M);
        d.setProblemStatement("Pb");
        d.setDescription("contexte non vide");       // couvre la branche description non vide
        when(diagramRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(d));

        String llm = String.join("\n",
                "Machines :",                          // en-tête : rest vide -> définit current
                "- réglage de la presse dérivé",       // rattachée à MACHINES
                "* usure des outils");                 // rattachée à MACHINES
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(llm, "ollama", 50, 500));

        List<IshikawaDto.SuggestedCause> res = ishikawaService.suggestCauses(id);

        assertThat(res).hasSize(2);
        assertThat(res).allMatch(s -> s.category() == CauseCategory.MACHINES);
        assertThat(res).extracting(IshikawaDto.SuggestedCause::label)
                .containsExactly("réglage de la presse dérivé", "usure des outils");
    }

    @Test
    void suggestCauses_nullLlmText_returnsEmpty() {
        UUID id = UUID.randomUUID();
        IshikawaDiagram d = new IshikawaDiagram();
        d.setTenantId(TENANT_ID);
        d.setMode(IshikawaMode.SIX_M);
        d.setProblemStatement("Pb");
        when(diagramRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(d));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(null, "ollama", 0, 10));

        assertThat(ishikawaService.suggestCauses(id)).isEmpty();
    }

    @Test
    void suggestCauses_diagramNotFound_throws() {
        UUID unknown = UUID.randomUUID();
        when(diagramRepository.findByIdAndTenantId(unknown, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ishikawaService.suggestCauses(unknown))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
        verifyNoInteractions(ai);
    }

    // --- createDiagram ---

    @Test
    void createDiagram_success_defaultsToSixMAndDraft() {
        IshikawaDto.CreateDiagramRequest request = new IshikawaDto.CreateDiagramRequest(
                "Défauts soudure ligne 3", "Description", null, OWNER_ID);

        IshikawaDiagram saved = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        saved.setProblemStatement(request.problemStatement());
        when(diagramRepository.save(any())).thenReturn(saved);

        IshikawaDto.DiagramResponse response = ishikawaService.createDiagram(request);

        assertThat(response.status()).isEqualTo(IshikawaStatus.DRAFT);
        assertThat(response.mode()).isEqualTo(IshikawaMode.SIX_M);
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        verify(diagramRepository).save(any(IshikawaDiagram.class));
    }

    @Test
    void createDiagram_withExplicitMode_usesIt() {
        IshikawaDto.CreateDiagramRequest request = new IshikawaDto.CreateDiagramRequest(
                "Pb", null, IshikawaMode.EIGHT_M, OWNER_ID);
        IshikawaDiagram saved = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.EIGHT_M);
        when(diagramRepository.save(any())).thenReturn(saved);

        IshikawaDto.DiagramResponse response = ishikawaService.createDiagram(request);

        assertThat(response.mode()).isEqualTo(IshikawaMode.EIGHT_M);
    }

    @Test
    void createDiagram_missingTenant_throws() {
        TenantContext.clear();
        IshikawaDto.CreateDiagramRequest request = new IshikawaDto.CreateDiagramRequest(
                "Pb", null, null, OWNER_ID);

        assertThatThrownBy(() -> ishikawaService.createDiagram(request))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(diagramRepository);
    }

    // --- findAll ---

    @Test
    void findAll_noStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByTenantId(TENANT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(diagram)));

        Page<IshikawaDto.DiagramResponse> result = ishikawaService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(diagramRepository, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void findAll_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.VALIDATED, IshikawaMode.SIX_M);
        when(diagramRepository.findByTenantIdAndStatus(TENANT_ID, IshikawaStatus.VALIDATED, pageable))
                .thenReturn(new PageImpl<>(List.of(diagram)));

        Page<IshikawaDto.DiagramResponse> result = ishikawaService.findAll(IshikawaStatus.VALIDATED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(IshikawaStatus.VALIDATED);
    }

    // --- findById ---

    @Test
    void findById_found() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaDto.DiagramResponse response = ishikawaService.findById(diagram.getId());

        assertThat(response.id()).isEqualTo(diagram.getId());
    }

    @Test
    void findById_notFound_throws() {
        UUID unknown = UUID.randomUUID();
        when(diagramRepository.findByIdAndTenantId(unknown, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ishikawaService.findById(unknown))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_throws() {
        IshikawaDiagram other = buildDiagram(OTHER_TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(other.getId(), TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ishikawaService.findById(other.getId()))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
    }

    // --- updateDiagram ---

    @Test
    void updateDiagram_updatesFields() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(diagramRepository.save(diagram)).thenReturn(diagram);

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                "Nouveau problème", "Nouvelle description", IshikawaMode.SEVEN_M, IshikawaStatus.IN_REVIEW);

        IshikawaDto.DiagramResponse response = ishikawaService.updateDiagram(diagram.getId(), request);

        assertThat(diagram.getProblemStatement()).isEqualTo("Nouveau problème");
        assertThat(diagram.getMode()).isEqualTo(IshikawaMode.SEVEN_M);
        assertThat(diagram.getStatus()).isEqualTo(IshikawaStatus.IN_REVIEW);
        assertThat(response).isNotNull();
    }

    @Test
    void updateDiagram_archived_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.ARCHIVED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                "x", null, null, null);

        assertThatThrownBy(() -> ishikawaService.updateDiagram(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("Archived");
    }

    @Test
    void updateDiagram_invalidStatusTransition_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        // DRAFT -> VALIDATED est interdit (il faut passer par IN_REVIEW)
        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                null, null, null, IshikawaStatus.VALIDATED);

        assertThatThrownBy(() -> ishikawaService.updateDiagram(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateDiagram_modeChangeRemovesCategory_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.EIGHT_M);
        IshikawaCause moneyCause = buildCause(diagram, CauseCategory.MONEY);
        diagram.getCauses().add(moneyCause);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                null, null, IshikawaMode.SIX_M, null);

        assertThatThrownBy(() -> ishikawaService.updateDiagram(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("Cannot switch to mode");
    }

    @Test
    void updateDiagram_statusInReviewToDraft_allowed() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.IN_REVIEW, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(diagramRepository.save(diagram)).thenReturn(diagram);

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                null, null, null, IshikawaStatus.DRAFT);

        ishikawaService.updateDiagram(diagram.getId(), request);

        assertThat(diagram.getStatus()).isEqualTo(IshikawaStatus.DRAFT);
    }

    @Test
    void updateDiagram_validatedToArchived_allowed() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.VALIDATED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(diagramRepository.save(diagram)).thenReturn(diagram);

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                null, null, null, IshikawaStatus.ARCHIVED);

        ishikawaService.updateDiagram(diagram.getId(), request);

        assertThat(diagram.getStatus()).isEqualTo(IshikawaStatus.ARCHIVED);
    }

    // --- deleteDiagram ---

    @Test
    void deleteDiagram_draft_success() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        ishikawaService.deleteDiagram(diagram.getId());

        verify(diagramRepository).delete(diagram);
    }

    @Test
    void deleteDiagram_validated_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.VALIDATED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        assertThatThrownBy(() -> ishikawaService.deleteDiagram(diagram.getId()))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("Validated");

        verify(diagramRepository, never()).delete(any(IshikawaDiagram.class));
    }

    @Test
    void deleteDiagram_notFound_throws() {
        UUID unknown = UUID.randomUUID();
        when(diagramRepository.findByIdAndTenantId(unknown, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ishikawaService.deleteDiagram(unknown))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
    }

    // --- addCause ---

    @Test
    void addCause_success() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaCause saved = buildCause(diagram, CauseCategory.METHODS);
        when(causeRepository.save(any())).thenReturn(saved);

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "Procédure inadéquate", "Détail", null, 0.4);

        IshikawaDto.CauseResponse response = ishikawaService.addCause(diagram.getId(), request);

        assertThat(response.category()).isEqualTo(CauseCategory.METHODS);
        verify(causeRepository).save(any(IshikawaCause.class));
    }

    @Test
    void addCause_categoryNotAllowedByMode_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        // SIX_M n'autorise pas MONEY
        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.MONEY, "Budget", null, null, null);

        assertThatThrownBy(() -> ishikawaService.addCause(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("not allowed by mode");
    }

    @Test
    void addCause_archivedDiagram_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.ARCHIVED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "Méthode", null, null, null);

        assertThatThrownBy(() -> ishikawaService.addCause(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void addCause_withParent_inheritsCategory() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        IshikawaCause parent = buildCause(diagram, CauseCategory.METHODS);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(parent.getId(), diagram.getId()))
                .thenReturn(Optional.of(parent));
        when(causeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "Sous-cause", null, parent.getId(), null);

        IshikawaDto.CauseResponse response = ishikawaService.addCause(diagram.getId(), request);

        assertThat(response.parentId()).isEqualTo(parent.getId());
    }

    @Test
    void addCause_parentCategoryMismatch_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        IshikawaCause parent = buildCause(diagram, CauseCategory.METHODS);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(parent.getId(), diagram.getId()))
                .thenReturn(Optional.of(parent));

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.MANPOWER, "Sous-cause", null, parent.getId(), null);

        assertThatThrownBy(() -> ishikawaService.addCause(diagram.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("must match parent category");
    }

    @Test
    void addCause_parentNotFound_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        UUID unknownParent = UUID.randomUUID();

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(unknownParent, diagram.getId()))
                .thenReturn(Optional.empty());

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "x", null, unknownParent, null);

        assertThatThrownBy(() -> ishikawaService.addCause(diagram.getId(), request))
                .isInstanceOf(IshikawaCauseNotFoundException.class);
    }

    @Test
    void addCause_diagramNotFound_throws() {
        UUID unknown = UUID.randomUUID();
        when(diagramRepository.findByIdAndTenantId(unknown, TENANT_ID)).thenReturn(Optional.empty());

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "x", null, null, null);

        assertThatThrownBy(() -> ishikawaService.addCause(unknown, request))
                .isInstanceOf(IshikawaDiagramNotFoundException.class);
    }

    // --- updateCause ---

    @Test
    void updateCause_success() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SEVEN_M);
        IshikawaCause cause = buildCause(diagram, CauseCategory.METHODS);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(cause.getId(), diagram.getId()))
                .thenReturn(Optional.of(cause));
        when(causeRepository.save(cause)).thenReturn(cause);

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                CauseCategory.MANAGEMENT, "Nouveau libellé", "Nouvelle desc", 0.85);

        ishikawaService.updateCause(diagram.getId(), cause.getId(), request);

        assertThat(cause.getCategory()).isEqualTo(CauseCategory.MANAGEMENT);
        assertThat(cause.getLabel()).isEqualTo("Nouveau libellé");
        assertThat(cause.getRootCauseScore()).isEqualTo(0.85);
    }

    @Test
    void updateCause_categoryNotAllowed_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        IshikawaCause cause = buildCause(diagram, CauseCategory.METHODS);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(cause.getId(), diagram.getId()))
                .thenReturn(Optional.of(cause));

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                CauseCategory.MONEY, null, null, null);

        assertThatThrownBy(() -> ishikawaService.updateCause(diagram.getId(), cause.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("not allowed by mode");
    }

    @Test
    void updateCause_categoryMismatchWithParent_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        IshikawaCause parent = buildCause(diagram, CauseCategory.METHODS);
        IshikawaCause child = buildCause(diagram, CauseCategory.METHODS);
        child.setParent(parent);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(child.getId(), diagram.getId()))
                .thenReturn(Optional.of(child));

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                CauseCategory.MANPOWER, null, null, null);

        assertThatThrownBy(() -> ishikawaService.updateCause(diagram.getId(), child.getId(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("must match parent category");
    }

    @Test
    void updateCause_archivedDiagram_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.ARCHIVED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                null, "x", null, null);

        assertThatThrownBy(() -> ishikawaService.updateCause(diagram.getId(), UUID.randomUUID(), request))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void updateCause_notFound_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        UUID unknownCause = UUID.randomUUID();

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(unknownCause, diagram.getId()))
                .thenReturn(Optional.empty());

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                null, "x", null, null);

        assertThatThrownBy(() -> ishikawaService.updateCause(diagram.getId(), unknownCause, request))
                .isInstanceOf(IshikawaCauseNotFoundException.class);
    }

    // --- deleteCause ---

    @Test
    void deleteCause_success() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        IshikawaCause cause = buildCause(diagram, CauseCategory.METHODS);

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(cause.getId(), diagram.getId()))
                .thenReturn(Optional.of(cause));

        ishikawaService.deleteCause(diagram.getId(), cause.getId());

        verify(causeRepository).delete(cause);
    }

    @Test
    void deleteCause_archivedDiagram_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.ARCHIVED, IshikawaMode.SIX_M);
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));

        assertThatThrownBy(() -> ishikawaService.deleteCause(diagram.getId(), UUID.randomUUID()))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void deleteCause_causeNotFound_throws() {
        IshikawaDiagram diagram = buildDiagram(TENANT_ID, IshikawaStatus.DRAFT, IshikawaMode.SIX_M);
        UUID unknown = UUID.randomUUID();

        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT_ID))
                .thenReturn(Optional.of(diagram));
        when(causeRepository.findByIdAndDiagramId(unknown, diagram.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ishikawaService.deleteCause(diagram.getId(), unknown))
                .isInstanceOf(IshikawaCauseNotFoundException.class);
    }

    // --- helpers ---

    private IshikawaDiagram buildDiagram(UUID tenantId, IshikawaStatus status, IshikawaMode mode) {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setId(UUID.randomUUID());
        diagram.setTenantId(tenantId);
        diagram.setProblemStatement("Problème test");
        diagram.setDescription("Description test");
        diagram.setMode(mode);
        diagram.setStatus(status);
        diagram.setOwnerId(OWNER_ID);
        diagram.setCreatedAt(Instant.now());
        diagram.setUpdatedAt(Instant.now());
        return diagram;
    }

    private IshikawaCause buildCause(IshikawaDiagram diagram, CauseCategory category) {
        IshikawaCause cause = new IshikawaCause();
        cause.setId(UUID.randomUUID());
        cause.setDiagram(diagram);
        cause.setCategory(category);
        cause.setLabel("Cause test");
        cause.setCreatedAt(Instant.now());
        cause.setUpdatedAt(Instant.now());
        return cause;
    }
}
