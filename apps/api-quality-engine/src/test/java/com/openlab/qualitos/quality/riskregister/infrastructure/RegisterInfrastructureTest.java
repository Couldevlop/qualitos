package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.riskregister.application.RiskCapaGateway;
import com.openlab.qualitos.quality.riskregister.domain.Identification;
import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityActionStatus;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityDetails;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEvent;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEventType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterItemKind;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;
import com.openlab.qualitos.quality.riskregister.domain.Risk;
import com.openlab.qualitos.quality.riskregister.domain.RiskDetails;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Les adaptateurs : JPA, journal d'audit, pont CAPA. */
class RegisterInfrastructureTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");

    static Identification ident() {
        return new Identification("Fuite — « confidentiel » de M. Kone", RegisterType.QUALITY, "Production",
                "Usine A", "M. Kone", RegisterOrigin.FMEA, "PFMEA-7 #3",
                Set.of(RegisterRequirement.ISO_9001_6_1, RegisterRequirement.IATF_16949_6_1_2));
    }

    static Risk risque(UUID id) {
        Risk neuf = Risk.create(TENANT, "R-014", UUID.randomUUID(), new RiskDetails(ident(), "cause libre",
                "effet libre", 4, 3, 4, 2, null, null, LocalDate.of(2027, 1, 1), "critère"), ACTEUR, T0);
        return id == null ? neuf : new Risk(id, TENANT, neuf.getReference(), neuf.getSourceId(),
                neuf.getIdentification(), neuf.getCause(), neuf.getEffect(), neuf.getGross(), neuf.getResidual(),
                neuf.getDecision(), neuf.getStatus(), neuf.getNextReviewOn(), neuf.getEffectivenessCriterion(),
                ACTEUR, T0, T0);
    }

    // ---------- exigences ----------

    @Test
    void lesExigencesSeStockentEnUneColonneEtUnCodeInconnuEstIgnore() {
        String codes = RegisterRepositoryAdapters.encode(
                EnumSet.of(RegisterRequirement.IATF_16949_6_1_2, RegisterRequirement.ISO_9001_6_1));
        assertThat(codes).isEqualTo("ISO_9001_6_1,IATF_16949_6_1_2");

        assertThat(RegisterRepositoryAdapters.decode(codes + ", ISO_RETIREE"))
                .containsExactlyInAnyOrder(RegisterRequirement.ISO_9001_6_1, RegisterRequirement.IATF_16949_6_1_2);
        assertThat(RegisterRepositoryAdapters.decode(null)).isEmpty();
        assertThat(RegisterRepositoryAdapters.decode(" ")).isEmpty();
        assertThat(RegisterRepositoryAdapters.encode(EnumSet.noneOf(RegisterRequirement.class))).isEmpty();
    }

    // ---------- risques ----------

    @Test
    void unRisqueNeufSEcritEtSeRelitALIdentique() {
        RiskJpaRepository jpa = mock(RiskJpaRepository.class);
        UUID id = UUID.randomUUID();
        when(jpa.save(any())).thenAnswer(inv -> {
            RiskJpaEntity e = inv.getArgument(0);
            e.setId(id);
            return e;
        });
        RegisterRepositoryAdapters.Risks adapter = new RegisterRepositoryAdapters.Risks(jpa);
        Risk original = risque(null);

        Risk relu = adapter.save(original);

        assertThat(relu.getId()).isEqualTo(id);
        assertThat(relu.getReference()).isEqualTo("R-014");
        assertThat(relu.getSourceId()).isEqualTo(original.getSourceId());
        assertThat(relu.getIdentification()).isEqualTo(original.getIdentification());
        assertThat(relu.getGross()).isEqualTo(original.getGross());
        assertThat(relu.getResidual()).isEqualTo(original.getResidual());
        assertThat(relu.getNextReviewOn()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(relu.getEffectivenessCriterion()).isEqualTo("critère");
        assertThat(relu.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void unRisqueDisparuOuDUnAutreClientNeSeRecreePas() {
        RiskJpaRepository jpa = mock(RiskJpaRepository.class);
        UUID id = UUID.randomUUID();
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegisterRepositoryAdapters.Risks(jpa).save(risque(id)))
                .isInstanceOf(RegisterNotFoundException.class);
        verify(jpa, never()).save(any());
    }

    @Test
    void unRisqueExistantEstMisAJourDansSonClientSansToucherASaReference() {
        RiskJpaRepository jpa = mock(RiskJpaRepository.class);
        UUID id = UUID.randomUUID();
        RiskJpaEntity existant = new RiskJpaEntity();
        existant.setId(id);
        existant.setTenantId(TENANT);
        existant.setReference("R-014");
        existant.setCreatedAt(T0);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(existant));
        when(jpa.save(existant)).thenReturn(existant);

        Risk relu = new RegisterRepositoryAdapters.Risks(jpa).save(risque(id));

        assertThat(relu.getGross().code()).isEqualTo("4x3");
        assertThat(existant.getRequirements()).isEqualTo("ISO_9001_6_1,IATF_16949_6_1_2");
        assertThat(existant.getSourceId()).isNull();
    }

    @Test
    void lesLecturesPassentLeTenant() {
        RiskJpaRepository jpa = mock(RiskJpaRepository.class);
        RegisterRepositoryAdapters.Risks adapter = new RegisterRepositoryAdapters.Risks(jpa);
        UUID source = UUID.randomUUID();
        when(jpa.countByTenantId(TENANT)).thenReturn(3L);
        when(jpa.existsByTenantIdAndReference(TENANT, "R-001")).thenReturn(true);
        when(jpa.findByTenantId(TENANT)).thenReturn(List.of());
        when(jpa.findByTenantIdAndOriginAndSourceId(TENANT, RegisterOrigin.FMEA, source)).thenReturn(List.of());

        assertThat(adapter.countByTenant(TENANT)).isEqualTo(3);
        assertThat(adapter.referenceTaken(TENANT, "R-001")).isTrue();
        assertThat(adapter.findByTenant(TENANT)).isEmpty();
        assertThat(adapter.findBySource(TENANT, RegisterOrigin.FMEA, source)).isEmpty();
        assertThat(adapter.findByIdAndTenant(UUID.randomUUID(), TENANT)).isEmpty();
    }

    @Test
    void uneResiduelleIncompleteEnBaseSeRelitCommeAbsente() {
        RiskJpaEntity e = new RiskJpaEntity();
        e.setTenantId(TENANT);
        e.setReference("R-001");
        e.setTitle("t");
        e.setType(RegisterType.QUALITY);
        e.setProcess("p");
        e.setOwner("o");
        e.setOrigin(RegisterOrigin.DIRECT);
        e.setGrossSeverity(2);
        e.setGrossProbability(2);
        e.setResidualSeverity(2);
        e.setRequirements("");

        assertThat(RegisterRepositoryAdapters.Risks.toDomain(e).getResidual()).isNull();
    }

    // ---------- opportunités, actions, suivi ----------

    @Test
    void uneOpportuniteSEcritEtSeRelit() {
        OpportunityJpaRepository jpa = mock(OpportunityJpaRepository.class);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegisterRepositoryAdapters.Opportunities adapter = new RegisterRepositoryAdapters.Opportunities(jpa);
        Opportunity o = Opportunity.create(TENANT, "O-003", new OpportunityDetails(
                new Identification("Automatiser SPC", RegisterType.QUALITY, "Production", null, "Mme Diallo",
                        RegisterOrigin.AUDIT, "AUD-7", Set.of(RegisterRequirement.ISO_9001_10_3)),
                LocalDate.of(2027, 3, 31), "ctx", "bén", 4, 4, null, null, "crit"), ACTEUR, T0);

        Opportunity relue = adapter.save(o);

        assertThat(relue.getReference()).isEqualTo("O-003");
        assertThat(relue.getEvaluation().score()).isEqualTo(16);
        assertThat(relue.getIdentification().requirements()).containsExactly(RegisterRequirement.ISO_9001_10_3);
        assertThat(relue.getTargetDate()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(relue.getBenefitCriterion()).isEqualTo("crit");

        UUID id = UUID.randomUUID();
        Opportunity existante = new Opportunity(id, TENANT, "O-003", relue.getIdentification(), null, null, null,
                relue.getEvaluation(), relue.getDecision(), relue.getStatus(), null, ACTEUR, T0, T0);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.save(existante)).isInstanceOf(RegisterNotFoundException.class);
        when(jpa.countByTenantId(TENANT)).thenReturn(1L);
        assertThat(adapter.countByTenant(TENANT)).isEqualTo(1);
        assertThat(adapter.referenceTaken(TENANT, "O-001")).isFalse();
        assertThat(adapter.findByTenant(TENANT)).isEmpty();
        assertThat(adapter.findByIdAndTenant(id, TENANT)).isEmpty();
    }

    @Test
    void uneActionSEcritSeRelitEtSeSupprimeDansSonClient() {
        OpportunityActionJpaRepository jpa = mock(OpportunityActionJpaRepository.class);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegisterRepositoryAdapters.Actions adapter = new RegisterRepositoryAdapters.Actions(jpa);
        UUID opp = UUID.randomUUID();

        OpportunityAction a = adapter.save(OpportunityAction.open(TENANT, opp, 7, "Chiffrer", null, null, T0));
        assertThat(a.getNumber()).isEqualTo(7);
        assertThat(a.getStatus()).isEqualTo(OpportunityActionStatus.TO_START);

        UUID id = UUID.randomUUID();
        OpportunityActionJpaEntity e = new OpportunityActionJpaEntity();
        e.setId(id);
        e.setTenantId(TENANT);
        e.setOpportunityId(opp);
        e.setNumber(7);
        when(jpa.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(e));
        OpportunityAction existante = new OpportunityAction(id, TENANT, opp, 7, "Chiffrer", null,
                OpportunityActionStatus.DONE, T0, T0);
        assertThat(adapter.save(existante).getStatus()).isEqualTo(OpportunityActionStatus.DONE);
        adapter.delete(existante);
        verify(jpa).delete(e);

        when(jpa.maxNumber(TENANT)).thenReturn(7);
        assertThat(adapter.maxNumber(TENANT)).isEqualTo(7);
        when(jpa.findByTenantIdAndOpportunityId(TENANT, opp)).thenReturn(List.of(e));
        assertThat(adapter.findByOpportunity(TENANT, opp)).hasSize(1);
        assertThat(adapter.findByIdAndTenant(id, TENANT)).isPresent();

        UUID inconnue = UUID.randomUUID();
        assertThatThrownBy(() -> adapter.save(new OpportunityAction(inconnue, TENANT, opp, 8, "x", null,
                OpportunityActionStatus.DONE, T0, T0))).isInstanceOf(RegisterNotFoundException.class);
    }

    @Test
    void unEvenementSInsereToujoursNeuf() {
        RegisterEventJpaRepository jpa = mock(RegisterEventJpaRepository.class);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegisterRepositoryAdapters.Events adapter = new RegisterRepositoryAdapters.Events(jpa);
        UUID item = UUID.randomUUID();

        RegisterEvent e = adapter.save(RegisterEvent.of(TENANT, RegisterItemKind.RISK, item,
                RegisterEventType.RATING_CHANGED, "3x3", "4x3", null, ACTEUR, T0));

        assertThat(e.fromValue()).isEqualTo("3x3");
        assertThat(e.at()).isEqualTo(T0);
        when(jpa.findByTenantIdAndItemKindAndItemIdOrderByOccurredAtDesc(TENANT, RegisterItemKind.RISK, item))
                .thenReturn(List.of());
        assertThat(adapter.findByItem(TENANT, RegisterItemKind.RISK, item)).isEmpty();
    }

    // ---------- journal d'audit ----------

    @Test
    void laTraceDAuditNeContientAucunTexteLibre() {
        AuditEventService events = mock(AuditEventService.class);
        AuditLogRegisterPublisher publisher = new AuditLogRegisterPublisher(events);
        Risk r = risque(UUID.randomUUID());
        UUID capa = UUID.randomUUID();

        publisher.riskRecorded(r, ACTEUR);
        publisher.riskRevised(r, ACTEUR);
        publisher.riskCapaOpened(r, capa, ACTEUR);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(3)).recordForTenant(eq(TENANT), captor.capture());
        List<AuditEventDto.RecordEventRequest> traces = captor.getAllValues();
        assertThat(traces).extracting(AuditEventDto.RecordEventRequest::action).containsExactly(
                AuditLogRegisterPublisher.RISK_RECORDED, AuditLogRegisterPublisher.RISK_REVISED,
                AuditLogRegisterPublisher.RISK_CAPA_OPENED);
        for (AuditEventDto.RecordEventRequest t : traces) {
            assertThat(t.action()).matches("^[a-z][a-z0-9._-]{1,99}$");
            assertThat(t.resourceType()).matches("^[a-z][a-z0-9_-]{1,63}$");
            assertThat(t.payloadJson()).doesNotContain("Kone", "confidentiel", "cause libre", "Production");
            assertThat(t.payloadJson()).contains("\"reference\":\"R-014\"", "\"gross\":\"4x3\"", "\"residual\":\"4x2\"");
        }
        assertThat(traces.get(2).payloadJson()).contains(capa.toString());
    }

    @Test
    void lesTracesDOpportuniteEtDActionSontPubliees() {
        AuditEventService events = mock(AuditEventService.class);
        AuditLogRegisterPublisher publisher = new AuditLogRegisterPublisher(events);
        Opportunity o = Opportunity.create(TENANT, "O-001", new OpportunityDetails(
                new Identification("Titre libre", RegisterType.QUALITY, "P", null, "Mme Diallo", null, null, null),
                null, null, null, 3, 3, null, null, null), ACTEUR, T0);
        OpportunityAction a = OpportunityAction.open(TENANT, UUID.randomUUID(), 2, "Action libre", null, null, T0);
        Risk sansResiduelle = Risk.create(TENANT, "R-002", new RiskDetails(ident(), "Usure buse", "Soudure non conforme", 2, 2, null, null,
                null, null, null, null), ACTEUR, T0);

        publisher.opportunityRecorded(o, ACTEUR);
        publisher.opportunityRevised(o, ACTEUR);
        publisher.actionRecorded(a, ACTEUR);
        publisher.actionRevised(a, ACTEUR);
        publisher.actionDeleted(a, ACTEUR);
        publisher.riskRecorded(sansResiduelle, ACTEUR);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(6)).recordForTenant(eq(TENANT), captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(t ->
                assertThat(t.payloadJson()).doesNotContain("libre", "Diallo"));
        assertThat(captor.getAllValues().get(5).payloadJson()).contains("\"residual\":null");
    }

    // ---------- pont CAPA ----------

    @Test
    void laCapaOuverteEstDeLaNatureChoisieDeSourceRisqueEtPorteSonAction() {
        CapaService capaService = mock(CapaService.class);
        CapaCaseRepository capaCases = mock(CapaCaseRepository.class);
        UUID capaId = UUID.randomUUID();
        CapaDto.CaseResponse reponse = new CapaDto.CaseResponse(capaId, TENANT, "Carte SPC", "desc",
                CapaType.PREVENTIVE, CapaCriticity.HIGH, CapaStatus.OPEN, CapaSourceType.RISK, "R-014", ACTEUR,
                null, LocalDate.of(2026, 12, 1), null, null, null, null, null, null, null, null, T0, T0,
                List.of(), null, List.of());
        when(capaService.createCase(any())).thenReturn(reponse);
        when(capaService.addAction(eq(capaId), any())).thenReturn(new CapaDto.ActionResponse(UUID.randomUUID(),
                capaId, "Carte SPC", "desc", CapaActionStatus.PENDING, CapaActionType.CORRECTIVE, null,
                "A. Diallo", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 1), null, T0, T0));
        Risk r = risque(UUID.randomUUID());

        RiskCapaGateway.LinkedCapa capa = new CapaRiskGateway(capaService, capaCases)
                .open(r, "Carte SPC", "desc", RiskCapaKind.CORRECTIVE, "A. Diallo", LocalDate.of(2026, 12, 1),
                        ACTEUR);

        ArgumentCaptor<CapaDto.CreateCaseRequest> captor = ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(captor.capture());
        CapaDto.CreateCaseRequest demande = captor.getValue();
        assertThat(demande.type()).isEqualTo(CapaType.CORRECTIVE);
        assertThat(demande.dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(demande.sourceType()).isEqualTo(CapaSourceType.RISK);
        assertThat(demande.sourceRef()).isEqualTo("R-014");
        assertThat(demande.criticity()).isEqualTo(CapaCriticity.HIGH);
        assertThat(demande.ownerId()).isEqualTo(ACTEUR);
        assertThat(capa.id()).isEqualTo(capaId);
        assertThat(capa.status()).isEqualTo("OPEN");
        assertThat(capa.kind()).isEqualTo(RiskCapaKind.CORRECTIVE);
        assertThat(capa.assignee()).isEqualTo("A. Diallo");

        // Le dossier naît avec SON action : même nature, même échéance, confiée au responsable.
        ArgumentCaptor<CapaDto.ActionRequest> action = ArgumentCaptor.forClass(CapaDto.ActionRequest.class);
        verify(capaService).addAction(eq(capaId), action.capture());
        assertThat(action.getValue().title()).isEqualTo("Carte SPC");
        assertThat(action.getValue().actionType()).isEqualTo(CapaActionType.CORRECTIVE);
        assertThat(action.getValue().assigneeName()).isEqualTo("A. Diallo");
        assertThat(action.getValue().dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void uneCapaPreventiveDonneUnDossierEtUneActionPreventives() {
        assertThat(CapaRiskGateway.typeDossier(RiskCapaKind.PREVENTIVE)).isEqualTo(CapaType.PREVENTIVE);
        assertThat(CapaRiskGateway.typeAction(RiskCapaKind.PREVENTIVE)).isEqualTo(CapaActionType.PREVENTIVE);
        assertThat(CapaRiskGateway.nature(CapaType.CORRECTIVE)).isEqualTo(RiskCapaKind.CORRECTIVE);
        // Les dossiers ouverts avant le choix étaient tous préventifs.
        assertThat(CapaRiskGateway.nature(CapaType.PREVENTIVE)).isEqualTo(RiskCapaKind.PREVENTIVE);
    }

    @Test
    void lesCapaLieesSeRetrouventParLaReferenceDuRisqueDansSonClient() {
        CapaCaseRepository capaCases = mock(CapaCaseRepository.class);
        CapaCase c = new CapaCase();
        c.setId(UUID.randomUUID());
        c.setTitle("Carte SPC");
        c.setStatus(CapaStatus.IN_PROGRESS);
        c.setType(CapaType.CORRECTIVE);
        CapaAction sansNom = new CapaAction();
        sansNom.setAssigneeName(" ");
        CapaAction nommee = new CapaAction();
        nommee.setAssigneeName("A. Diallo");
        c.getActions().addAll(List.of(sansNom, nommee));
        when(capaCases.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(TENANT, CapaSourceType.RISK, "R-014"))
                .thenReturn(List.of(c));

        List<RiskCapaGateway.LinkedCapa> liees = new CapaRiskGateway(mock(CapaService.class), capaCases)
                .linkedTo(risque(UUID.randomUUID()));

        assertThat(liees).singleElement().satisfies(l -> {
            assertThat(l.title()).isEqualTo("Carte SPC");
            assertThat(l.status()).isEqualTo("IN_PROGRESS");
            assertThat(l.kind()).isEqualTo(RiskCapaKind.CORRECTIVE);
            assertThat(l.assignee()).isEqualTo("A. Diallo");
        });
    }

    @Test
    void chaqueNiveauDonneSaCriticite() {
        assertThat(List.of(1, 5, 10, 15).stream().map(score -> {
            int g = score == 1 ? 1 : score == 5 ? 5 : score == 10 ? 5 : 5;
            int p = score == 1 ? 1 : score == 5 ? 1 : score == 10 ? 2 : 3;
            Risk r = Risk.create(TENANT, "R-1", new RiskDetails(ident(), "Usure buse", "Soudure non conforme", g, p, null, null, null, null,
                    null, null), ACTEUR, T0);
            return CapaRiskGateway.criticite(r);
        }).toList()).containsExactly(CapaCriticity.LOW, CapaCriticity.MEDIUM, CapaCriticity.HIGH,
                CapaCriticity.CRITICAL);
    }
}
