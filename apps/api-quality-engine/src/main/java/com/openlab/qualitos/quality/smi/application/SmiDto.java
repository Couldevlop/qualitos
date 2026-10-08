package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.CoverageStatus;
import com.openlab.qualitos.quality.smi.domain.Deadline;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Les vues du tableau de bord SMI. Des codes et des nombres ; les mots se composent à l'écran. */
public final class SmiDto {

    private SmiDto() {}

    /**
     * La page d'accueil du SMI.
     *
     * @param selected la norme filtrée, {@code null} pour « Tous »
     * @param compliance {@code null} quand le client n'a adopté aucune norme
     */
    public record Dashboard(
            List<StandardRef> standards,
            String selected,
            Compliance compliance,
            OverdueActions overdueActions,
            MajorRisks majorRisks,
            RiskMatrix riskMatrix,
            NextAudit nextAudit,
            List<Upcoming> thisWeek) {}

    public record StandardRef(UUID adoptionId, String code, String name) {}

    /** Conformité globale (moyenne des normes, ou la norme filtrée) et barre par norme. */
    public record Compliance(Integer global, List<StandardScore> perStandard) {}

    public record StandardScore(String code, String name, int score) {}

    /** Actions CAPA en retard, toutes normes confondues : une action n'est rattachée à aucune norme. */
    public record OverdueActions(long total, long critical) {}

    /** Risques ouverts de niveau ≥ 10 (élevé et critique). */
    public record MajorRisks(int total, int critical, int high) {}

    /** {@code gross.get(g-1).get(p-1)} risques de gravité g et probabilité p. */
    public record RiskMatrix(int open, List<List<Integer>> gross, List<List<Integer>> residual) {}

    public record NextAudit(UUID id, String reference, String title, String type, String standard,
                            LocalDate scheduledOn, long daysUntil) {}

    /** Une échéance de la semaine ; {@code daysLeft} négatif = échue. */
    public record Upcoming(Deadline.Kind kind, UUID targetId, String reference, String title,
                           LocalDate dueOn, long daysLeft) {}

    // ---------- matrice des exigences ----------

    /** Chapitres communs × normes adoptées : une preuve, plusieurs référentiels. */
    public record RequirementsMatrix(List<StandardRef> standards, List<ChapterRow> rows) {}

    public record ChapterRow(String chapter, List<String> modules, List<Cell> cells) {}

    /** Une case : la norme, son état, et de quoi l'expliquer. */
    public record Cell(String standardCode, CoverageStatus status, int covered, int total, String sectionTitle) {}
}
