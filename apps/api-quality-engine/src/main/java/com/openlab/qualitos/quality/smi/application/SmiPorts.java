package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.Deadline;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ce que le tableau de bord SMI lit, module par module.
 *
 * <p>Le SMI n'a pas de données à lui : il RELIT celles des modules qui les
 * produisent — Standards Hub, CAPA, registre des risques, audits, calibration,
 * changements. Chaque port est une lecture, dans le client du jeton ; aucun
 * n'écrit. Les adaptateurs passent par les services des modules quand ils
 * existent, pour hériter de leurs règles (cloisonnement, visibilité).
 */
public final class SmiPorts {

    private SmiPorts() {}

    /** Les normes que le client a adoptées, et leur alignement. */
    public interface Standards {

        List<AdoptedStandard> adopted();

        Optional<Alignment> alignment(UUID adoptionId);
    }

    public record AdoptedStandard(UUID adoptionId, String code, String name) {}

    public record Alignment(double overallScore, List<SectionCoverage> sections) {}

    /** Un chapitre d'une norme : combien d'exigences, combien prouvées. */
    public record SectionCoverage(String code, String title, int covered, int total) {}

    /** Les actions CAPA : celles en retard, celles à mener bientôt. */
    public interface Actions {

        OverdueActions overdue(LocalDate today);

        List<Deadline> dueBy(LocalDate until, int limit);
    }

    public record OverdueActions(long total, long critical) {}

    /** Les risques encore ouverts du registre. */
    public interface Risks {

        List<OpenRisk> open();
    }

    public record OpenRisk(UUID id, String reference, String title, int grossSeverity, int grossProbability,
                           Integer residualSeverity, Integer residualProbability, String grossLevel,
                           List<String> requirements, LocalDate nextReviewOn) {}

    /** Les audits planifiés. */
    public interface Audits {

        List<PlannedAudit> planned(int horizonDays);
    }

    public record PlannedAudit(UUID id, String reference, String title, String type, String standard,
                               LocalDate scheduledOn) {}

    /** Les autres échéances : étalonnages et changements en attente de validation. */
    public interface Deadlines {

        List<Deadline> dueBy(LocalDate until, int limit);
    }
}
