package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rappel d'échéance des audits planifiés (§4.4) : notification interne pour le
 * pilote et l'audité, et courriel quand une adresse est renseignée et que la
 * brique SMTP est active.
 *
 * <p><b>Exécution hors requête.</b> L'appelant est un ordonnanceur : il n'y a ni
 * {@code TenantContext}, ni utilisateur authentifié. Chaque audit est donc traité
 * avec le {@code tenantId} porté par sa propre ligne, jamais avec un contexte
 * ambiant. C'est le piège central de ce genre de travail : un service écrit pour
 * le chemin HTTP échouerait ici, ou — bien pire — écrirait dans le mauvais tenant
 * si un thread réutilisé en avait gardé un.
 *
 * <p><b>Idempotence.</b> Le droit d'envoyer se réserve par un UPDATE conditionnel
 * ({@link AuditPlanRepository#claimReminder}) AVANT tout envoi. L'arbitrage est
 * assumé : au pire un rappel est perdu (panne entre la réservation et l'envoi),
 * jamais dupliqué. Le contraire — envoyer puis marquer — garantirait le doublon à
 * chaque incident, et un rappel qui arrive deux fois fait douter de tous les autres.
 */
@Service
public class AuditReminderService {

    private static final Logger log = LoggerFactory.getLogger(AuditReminderService.class);

    private final AuditPlanRepository plans;
    private final NotificationRepository notifications;
    private final ObjectProvider<AuditReminderMailer> mailerProvider;
    private final AuditReminderProperties props;
    private final Clock clock;

    public AuditReminderService(AuditPlanRepository plans,
                                NotificationRepository notifications,
                                ObjectProvider<AuditReminderMailer> mailerProvider,
                                AuditReminderProperties props,
                                Clock clock) {
        this.plans = plans;
        this.notifications = notifications;
        this.mailerProvider = mailerProvider;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Traite tous les audits dont l'échéance entre dans la fenêtre de rappel, tous
     * tenants confondus.
     *
     * <p>Pas de {@code @Transactional} sur cette méthode, volontairement : une
     * transaction englobante ferait dépendre la réservation du succès de l'envoi
     * — un serveur SMTP en panne annulerait les marques et le passage suivant
     * réenverrait tout. La réservation porte sa propre transaction (cf. repository),
     * de sorte qu'elle est acquise et validée avant qu'on tente quoi que ce soit.
     *
     * @return le compte-rendu du passage (utile aux tests et au journal).
     */
    public Report sendDueReminders() {
        LocalDate today = LocalDate.now(clock);
        LocalDate horizon = today.plusDays(props.getDaysBefore());

        List<AuditPlan> due = plans.findDueForReminder(
                AuditStatus.PLANNED, today, horizon, PageRequest.of(0, props.getBatchSize()));

        int reminded = 0;
        int alreadyClaimed = 0;
        int mailed = 0;
        int mailFailures = 0;

        for (AuditPlan plan : due) {
            Instant now = clock.instant();
            int claimed;
            try {
                claimed = plans.claimReminder(plan.getId(), now);
            } catch (RuntimeException e) {
                // Un audit qui échoue ne doit pas emporter les suivants : le même
                // travers que l'ordonnanceur d'ancrage a déjà appris à éviter.
                log.error("[audit-reminder] réservation impossible pour l'audit {} : {}",
                        plan.getId(), e.getMessage());
                continue;
            }
            if (claimed != 1) {
                // Une autre réplique a gagné la course. Rien à signaler : c'est
                // exactement ce que la marque en base est là pour produire.
                alreadyClaimed++;
                continue;
            }

            long daysUntil = ChronoUnit.DAYS.between(today, plan.getScheduledDate());
            try {
                notifyInApp(plan, daysUntil, now);
                reminded++;
            } catch (RuntimeException e) {
                log.error("[audit-reminder] notification interne en échec pour l'audit {} : {}",
                        plan.getId(), e.getMessage());
            }

            switch (sendMail(plan, daysUntil)) {
                case SENT -> mailed++;
                case FAILED -> mailFailures++;
                case SKIPPED -> { /* ni adresse ni brique SMTP : rien à compter */ }
            }
        }

        if (reminded > 0 || mailFailures > 0) {
            log.info("[audit-reminder] {} rappel(s) posé(s), {} courriel(s) envoyé(s), "
                            + "{} échec(s) d'envoi, {} déjà pris par une autre réplique",
                    reminded, mailed, mailFailures, alreadyClaimed);
        }
        return new Report(due.size(), reminded, alreadyClaimed, mailed, mailFailures);
    }

    /**
     * Notifie le pilote et, s'il diffère, l'audité — chacun nommément.
     *
     * <p>Une diffusion à tout le tenant ({@code recipientUserId = null}) aurait été
     * plus simple, et aurait noyé chaque utilisateur sous les échéances des audits
     * des autres. Un rappel qu'on apprend à ignorer ne rappelle plus rien.
     */
    private void notifyInApp(AuditPlan plan, long daysUntil, Instant now) {
        for (String recipient : recipientsOf(plan)) {
            notifications.save(Notification.create(
                    UUID.randomUUID(),
                    plan.getTenantId(),
                    recipient,
                    NotificationType.WARNING,
                    subjectOf(plan),
                    bodyOf(plan, daysUntil),
                    "/audits/" + plan.getId(),
                    now));
        }
    }

    /** Pilote d'abord, audité ensuite ; jamais deux fois la même personne. */
    static List<String> recipientsOf(AuditPlan plan) {
        Set<String> recipients = new LinkedHashSet<>();
        if (plan.getLeadAuditorId() != null) {
            recipients.add(plan.getLeadAuditorId().toString());
        }
        if (plan.getAuditeeId() != null) {
            recipients.add(plan.getAuditeeId().toString());
        }
        return new ArrayList<>(recipients);
    }

    private MailOutcome sendMail(AuditPlan plan, long daysUntil) {
        String to = plan.getReminderEmail();
        if (to == null || to.isBlank()) {
            return MailOutcome.SKIPPED;
        }
        AuditReminderMailer mailer = mailerProvider.getIfAvailable();
        if (mailer == null) {
            // Brique SMTP non configurée : cas nominal, pas une anomalie. En DEBUG
            // pour ne pas remplir le journal d'un environnement volontairement muet.
            log.debug("[audit-reminder] envoi désactivé — rappel interne seul pour l'audit {}",
                    plan.getId());
            return MailOutcome.SKIPPED;
        }
        try {
            mailer.send(to, subjectOf(plan), bodyOf(plan, daysUntil));
            return MailOutcome.SENT;
        } catch (RuntimeException e) {
            // L'échec est journalisé SANS l'adresse (donnée personnelle, §22-9) et
            // sans relance : la réservation est déjà posée, et réessayer au passage
            // suivant produirait le doublon que tout ce dispositif écarte. La
            // notification interne, elle, est bien arrivée.
            log.error("[audit-reminder] envoi du courriel en échec pour l'audit {} : {}",
                    plan.getId(), e.getMessage());
            return MailOutcome.FAILED;
        }
    }

    static String subjectOf(AuditPlan plan) {
        return "Audit à préparer : " + plan.getTitle();
    }

    /**
     * Corps du rappel.
     *
     * <p>Texte en français et date au format ISO : ce message est produit hors
     * requête, sans en-tête {@code Accept-Language} ni utilisateur connecté, donc
     * sans locale à laquelle se fier. Le français est la langue de référence du
     * produit, et {@code 2026-09-05} se lit sans ambiguïté partout — contrairement
     * à {@code 05/09/2026}. Traduire ces deux chaînes suppose de savoir à qui l'on
     * écrit : c'est une évolution, pas un oubli.
     */
    static String bodyOf(AuditPlan plan, long daysUntil) {
        String echeance = daysUntil == 0
                ? "aujourd'hui"
                : (daysUntil == 1 ? "dans 1 jour" : "dans " + daysUntil + " jours");
        StringBuilder sb = new StringBuilder();
        sb.append("L'audit « ").append(plan.getTitle()).append(" » (type ").append(plan.getType())
                .append(") est planifié le ").append(plan.getScheduledDate())
                .append(", soit ").append(echeance).append(".\n");
        if (plan.getStandard() != null && !plan.getStandard().isBlank()) {
            sb.append("Référentiel visé : ").append(plan.getStandard()).append("\n");
        }
        if (plan.getScope() != null && !plan.getScope().isBlank()) {
            sb.append("Périmètre : ").append(plan.getScope()).append("\n");
        }
        sb.append("\nPréparez la checklist, les convocations et les preuves attendues.");
        return sb.toString();
    }

    /** Issue de la tentative d'envoi, distinguée du simple « pas d'adresse ». */
    enum MailOutcome { SENT, FAILED, SKIPPED }

    /** Compte-rendu d'un passage. {@code alreadyClaimed} > 0 = concurrence saine. */
    public record Report(int candidates, int reminded, int alreadyClaimed,
                         int mailed, int mailFailures) {}
}
