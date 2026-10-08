package com.openlab.qualitos.quality.smi.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.change.ChangeRequest;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.change.ChangeRequestStatus;
import com.openlab.qualitos.quality.change.ChangeRequestType;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.smi.application.SmiPorts;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import com.openlab.qualitos.quality.capa.CapaActionRepository;
import com.openlab.qualitos.quality.calibration.CalibrationEquipmentRepository;
import com.openlab.qualitos.quality.calibration.CalibrationPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les requêtes que le SMI a ajoutées aux dépôts CAPA et changements, jouées
 * sur le mapping JPA réel — un dépôt simulé rendrait ce qu'on lui donne et ne
 * dirait rien d'une jointure fausse ou d'un client mal cloisonné.
 *
 * <p>Même idiome que {@code IdeaCircleLazyLoadingTest} : contexte complet,
 * profil de test (H2 en mode PostgreSQL), chaque appel dans sa transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class SmiQueriesIntegrationTest {

    static final LocalDate J = LocalDate.of(2026, 10, 8);

    @Autowired CapaCaseRepository cases;
    @Autowired CapaActionRepository actions;
    @Autowired ChangeRequestRepository changes;
    @Autowired CalibrationPlanRepository plans;
    @Autowired CalibrationEquipmentRepository equipments;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private CapaCase dossier(UUID tenant, CapaStatus statut, CapaCriticity criticite, String ref) {
        CapaCase c = new CapaCase();
        c.setTenantId(tenant);
        c.setTitle("Dossier " + ref);
        c.setType(CapaType.PREVENTIVE);
        c.setCriticity(criticite);
        c.setStatus(statut);
        c.setSourceType(CapaSourceType.RISK);
        c.setSourceRef(ref);
        c.setOwnerId(UUID.randomUUID());
        return c;
    }

    private static void action(CapaCase c, String titre, CapaActionStatus statut, LocalDate echeance) {
        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle(titre);
        a.setStatus(statut);
        a.setDueDate(echeance);
        c.getActions().add(a);
    }

    @Test
    void lesActionsEnRetardSeComptentParClientHorsDossiersTerminesEtActionsFaites() {
        UUID tenant = UUID.randomUUID();
        CapaCase critique = dossier(tenant, CapaStatus.IN_PROGRESS, CapaCriticity.CRITICAL, "R-1");
        action(critique, "en retard critique", CapaActionStatus.PENDING, J.minusDays(3));
        action(critique, "faite", CapaActionStatus.DONE, J.minusDays(3));
        action(critique, "à venir", CapaActionStatus.PENDING, J.plusDays(3));
        CapaCase moyen = dossier(tenant, CapaStatus.OPEN, CapaCriticity.MEDIUM, "R-2");
        action(moyen, "en retard", CapaActionStatus.IN_PROGRESS, J.minusDays(1));
        action(moyen, "sans échéance", CapaActionStatus.PENDING, null);
        CapaCase clos = dossier(tenant, CapaStatus.CLOSED, CapaCriticity.CRITICAL, "R-3");
        action(clos, "d'un dossier clos", CapaActionStatus.PENDING, J.minusDays(9));
        CapaCase autreClient = dossier(UUID.randomUUID(), CapaStatus.OPEN, CapaCriticity.CRITICAL, "R-4");
        action(autreClient, "d'un autre client", CapaActionStatus.PENDING, J.minusDays(9));
        cases.saveAll(List.of(critique, moyen, clos, autreClient));

        TenantContext.setTenantId(tenant.toString());
        SmiAdapters.Actions port = new SmiAdapters.Actions(actions);

        assertThat(port.overdue(J)).isEqualTo(new SmiPorts.OverdueActions(2, 1));
        List<Deadline> semaine = port.dueBy(J.plusDays(7), 8);
        assertThat(semaine).extracting(Deadline::title).containsExactly("en retard critique", "en retard", "à venir");
        assertThat(semaine.get(0).targetId()).isEqualTo(critique.getId());
        assertThat(semaine.get(0).reference()).isEqualTo("R-1");
        assertThat(port.dueBy(J.plusDays(7), 1)).hasSize(1);
    }

    @Test
    void seulsLesChangementsEnAttenteDeValidationDuClientSontDesEcheances() {
        UUID tenant = UUID.randomUUID();
        changes.saveAll(List.of(
                changement(tenant, "MOC-1", ChangeRequestStatus.UNDER_REVIEW, J.plusDays(2)),
                changement(tenant, "MOC-2", ChangeRequestStatus.SUBMITTED, J.minusDays(1)),
                changement(tenant, "MOC-3", ChangeRequestStatus.APPROVED, J.plusDays(1)),
                changement(tenant, "MOC-4", ChangeRequestStatus.UNDER_REVIEW, J.plusDays(30)),
                changement(UUID.randomUUID(), "MOC-5", ChangeRequestStatus.UNDER_REVIEW, J)));

        TenantContext.setTenantId(tenant.toString());
        List<Deadline> echeances = new SmiAdapters.Deadlines(plans, equipments, changes).dueBy(J.plusDays(7), 8);

        assertThat(echeances).extracting(Deadline::reference).containsExactly("MOC-2", "MOC-1");
        assertThat(echeances).allSatisfy(d -> assertThat(d.kind()).isEqualTo(Deadline.Kind.CHANGE));
    }

    private static ChangeRequest changement(UUID tenant, String code, ChangeRequestStatus statut, LocalDate prevu) {
        ChangeRequest c = new ChangeRequest();
        c.setTenantId(tenant);
        c.setCode(code);
        c.setTitle("Changement " + code);
        c.setType(ChangeRequestType.PROCESS);
        c.setStatus(statut);
        c.setRequesterUserId(UUID.randomUUID());
        c.setPlannedFor(prevu);
        return c;
    }
}
