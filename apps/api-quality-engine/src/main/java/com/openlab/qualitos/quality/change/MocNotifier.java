package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.audit.AuditReminderMailer;
import com.openlab.qualitos.quality.change.domain.ValidationRole;
import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les notifications de la gestion des changements (ADR 0087), sur les canaux
 * existants : la cloche (notifications in-app adressées à UN utilisateur) et,
 * quand la brique SMTP est active, un courriel aux titulaires de rôle.
 *
 * <p><b>Une notification par personne et par demande.</b> Un utilisateur titulaire
 * de plusieurs rôles requis reçoit UNE notification qui liste ses rôles.
 *
 * <p><b>Le demandeur n'est jamais appelé à valider</b> : il ne peut pas statuer
 * sur sa propre demande (séparation des tâches), lui écrire « à valider » serait
 * une impasse.
 *
 * <p><b>Peu de données.</b> Le code, l'intitulé de la demande et les rôles ; ni
 * le nom du demandeur ni celui des validateurs. Le lien mène à la demande,
 * sélectionnée dans l'écran MOC : le détail se lit là, sous les droits de
 * l'utilisateur.
 *
 * <p>Le courriel est un complément : sans SMTP, aucun envoi et aucune erreur ; un
 * envoi en échec est journalisé sans l'adresse et n'empêche rien.
 */
@Component
public class MocNotifier {

    private static final Logger log = LoggerFactory.getLogger(MocNotifier.class);

    private final NotificationRepository notifications;
    private final ObjectProvider<AuditReminderMailer> mailer;
    private final Clock clock;

    public MocNotifier(NotificationRepository notifications, ObjectProvider<AuditReminderMailer> mailer, Clock clock) {
        this.notifications = notifications;
        this.mailer = mailer;
        this.clock = clock;
    }

    /**
     * Une personne à prévenir pour une demande : son adresse d'annuaire ({@code null}
     * si inconnue — la cloche seule la prévient) et les rôles qui la concernent.
     */
    public record Recipient(UUID userId, String address, List<String> roles) {}

    /**
     * Regroupe les titulaires de plusieurs rôles en destinataires uniques, le
     * demandeur exclu. L'ordre est celui des rôles, puis des titulaires.
     */
    public static List<Recipient> recipients(Map<String, List<ValidationRole.RoleHolder>> holdersByRole, UUID requester) {
        Map<UUID, Recipient> parPersonne = new LinkedHashMap<>();
        holdersByRole.forEach((role, holders) -> {
            for (ValidationRole.RoleHolder h : holders) {
                if (h.userId().equals(requester)) {
                    continue;
                }
                Recipient r = parPersonne.computeIfAbsent(h.userId(),
                        id -> new Recipient(id, h.mailAddress(), new ArrayList<>()));
                if (!r.roles().contains(role)) {
                    r.roles().add(role);
                }
            }
        });
        return List.copyOf(parPersonne.values());
    }

    /** 1 et 4 — Soumission (ou nouvelle soumission) : chaque titulaire des rôles requis. */
    public void awaitingValidation(ChangeRequest c, Collection<Recipient> recipients) {
        for (Recipient r : recipients) {
            String roles = String.join(", ", r.roles());
            String titre = c.getCode() + " attend votre validation (" + roleWord(r.roles()) + " " + roles + ")";
            send(c, r, NotificationType.WARNING, titre, "« " + c.getTitle() + " »");
        }
    }

    /** Rappel : la carte de ces rôles attend depuis {@code days} jours. */
    public void reminder(ChangeRequest c, Collection<Recipient> recipients, int days) {
        for (Recipient r : recipients) {
            String roles = String.join(", ", r.roles());
            String titre = "Rappel : " + c.getCode() + " attend votre validation depuis " + days + " jour"
                    + (days > 1 ? "s" : "") + " (" + roleWord(r.roles()) + " " + roles + ")";
            send(c, r, NotificationType.WARNING, titre, "« " + c.getTitle() + " »");
        }
    }

    /** 2 — Décision finale, au demandeur. Le motif du refus l'accompagne. */
    public void decided(ChangeRequest c, boolean approved, String comment) {
        if (approved) {
            inApp(c, c.getRequesterUserId(), NotificationType.SUCCESS,
                    c.getCode() + " est approuvée", "« " + c.getTitle() + " »");
        } else {
            inApp(c, c.getRequesterUserId(), NotificationType.ALERT,
                    c.getCode() + " est rejetée", "Motif du refus : " + comment);
        }
    }

    /**
     * 3 — Un refus clôt la validation : les titulaires des cartes encore en attente
     * n'ont plus rien à faire. Leurs notifications « à valider » sont marquées
     * lues (elles ne s'affichent plus comme une tâche), puis une information les
     * prévient.
     */
    public void validationClosed(ChangeRequest c, Collection<Recipient> recipients) {
        for (Recipient r : recipients) {
            retire(c, r.userId());
            inApp(c, r.userId(), NotificationType.INFO,
                    c.getCode() + " a été rejetée : votre validation n'est plus attendue",
                    "« " + c.getTitle() + " »");
        }
    }

    /**
     * Un titulaire du rôle a statué : ses co-titulaires et suppléants n'ont plus
     * rien à faire sur cette carte. Même geste qu'après un refus — la
     * notification « à valider » est marquée lue, puis une information les
     * prévient — sans quoi elle restait chez eux comme une tâche qu'ils ne
     * pouvaient plus accomplir (la carte répond « déjà statué »).
     */
    public void roleDecided(ChangeRequest c, String roleLabel, Collection<UUID> users) {
        for (UUID u : users) {
            retire(c, u);
            inApp(c, u, NotificationType.INFO,
                    c.getCode() + " : le rôle " + roleLabel + " a statué, votre validation n'est plus attendue",
                    "« " + c.getTitle() + " »");
        }
    }

    /** Celui qui vient de statuer n'a plus de tâche sur cette demande. */
    public void retire(ChangeRequest c, UUID userId) {
        notifications.markReadForRecipientAndLink(c.getTenantId(), userId.toString(), link(c), clock.instant());
    }

    static String link(ChangeRequest c) {
        return "/changes/" + c.getId();
    }

    private static String roleWord(List<String> roles) {
        return roles.size() > 1 ? "rôles" : "rôle";
    }

    private void send(ChangeRequest c, Recipient r, NotificationType type, String title, String body) {
        inApp(c, r.userId(), type, title, body);
        mail(r.address(), title, body + "\n\n" + link(c));
    }

    private void inApp(ChangeRequest c, UUID userId, NotificationType type, String title, String body) {
        notifications.save(Notification.create(UUID.randomUUID(), c.getTenantId(), userId.toString(),
                type, title, body, link(c), clock.instant()));
    }

    private void mail(String to, String subject, String body) {
        if (to == null || !to.contains("@")) {
            return;
        }
        AuditReminderMailer m = mailer.getIfAvailable();
        if (m == null) {
            return;
        }
        try {
            m.send(to, subject, body);
        } catch (RuntimeException e) {
            // Sans l'adresse (donnée personnelle) : la notification interne est partie.
            log.error("[moc] envoi du courriel en échec : {}", e.getMessage());
        }
    }
}
